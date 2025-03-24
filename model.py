import spacy
import re
from collections import defaultdict
import pandas as pd
import nltk
from nltk.sentiment.vader import SentimentIntensityAnalyzer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
import sys
from pathlib import Path

# Use pip to install: pip install --upgrade spacy pandas nltk scikit-learn numpy

# Download VADER lexicon
nltk.download('vader_lexicon', quiet=True)

# Load NLP tools with error handling
try:
    nlp = spacy.load("en_core_web_sm")
except OSError:
    print("SpaCy model 'en_core_web_sm' not found. Run: python -m spacy download en_core_web_sm")
    sys.exit(1)
sid = SentimentIntensityAnalyzer()

# Define dictionaries
interests_dict = {
    "sports": ["sports", "hiking", "gym", "exercise", "yoga"],
    "dining": ["dining", "food", "restaurant", "meal"],
    "movies": ["movie", "tv", "film", "show"],
    "art": ["art", "museum", "theater", "concert"],
    "reading": ["book", "read", "literature", "novel"],
    "music": ["music", "concert", "symphony"],
    "clubbing": ["club", "dance", "nightlife"],
    "shopping": ["shopping", "retail"],
    "gaming": ["gaming", "game", "video game"]
}

traits_dict = {
    "attractiveness": ["attractive", "looks", "beauty", "charming"],
    "intelligence": ["smart", "intelligent", "brain", "wise"],
    "humor": ["funny", "humor", "wit", "laugh"],
    "sincerity": ["sincere", "honest", "genuine", "authentic"],
    "ambition": ["ambitious", "driven", "goals"]
}

preferences_dict = traits_dict.copy()
preferences_dict["shared_interests"] = ["shared", "common", "similar", "hobby"]
preferences_dict["religion"] = ["religion", "faith", "belief", "spiritual"]

intensity_modifiers = {
    "love": 1.5, "obsessed": 2.0, "passionate": 1.5, "enjoy": 1.2, "like": 1.0,
    "sometimes": 0.8, "occasional": 0.7, "not": -1.0, "hate": -1.5, "dislike": -1.2
}

# Preprocessing Functions
def preprocess(text):
    if not isinstance(text, str):
        return nlp("")
    return nlp(text.lower())

def extract_interests(text):
    doc = preprocess(text)
    interests = defaultdict(float)
    for sentence in doc.sents:
        sentiment = sid.polarity_scores(sentence.text)["compound"]
        base_score = 5.0
        for category, keywords in interests_dict.items():
            for token in sentence:
                if token.text in keywords:
                    score = base_score + (sentiment * 5)
                    for child in token.children:
                        if child.text in intensity_modifiers:
                            score *= intensity_modifiers[child.text]
                    score = round(max(0, min(10, score)))
                    interests[category] = max(interests[category], score)
    return dict(interests)

def extract_traits(text):
    doc = preprocess(text)
    traits = {}
    ratings = re.findall(r"(\d{1,2}/\d{1,2})\s*(\w+)", str(text))
    for rating, trait in ratings:
        score = int(rating.split("/")[0])
        traits[trait] = min(score, 10)
    for sentence in doc.sents:
        sentiment = sid.polarity_scores(sentence.text)["compound"]
        base_score = 5.0
        for trait, keywords in traits_dict.items():
            for token in sentence:
                if token.text in keywords and trait not in traits:
                    score = base_score + (sentiment * 5)
                    for child in token.children:
                        if child.text in intensity_modifiers:
                            score *= intensity_modifiers[child.text]
                    score = round(max(0, min(10, score)))
                    traits[trait] = score
    return traits

def extract_preferences(text):
    doc = preprocess(text)
    preferences = {pref: pd.NA for pref in ["shared_interests", "humor", "attractiveness", "intelligence", "ambition", "religion"]}
    text_lower = str(text).lower()
    if any(keyword in text_lower for keyword in preferences_dict["religion"]):
        preferences["religion"] = "Religious"
    for sentence in doc.sents:
        sent_text = sentence.text.lower()
        if "shared" in sent_text and ("interest" in sent_text or "hobby" in sent_text):
            preferences["shared_interests"] = "Yes"
        for trait, keywords in preferences_dict.items():
            if trait not in ["shared_interests", "religion"] and any(keyword in sent_text for keyword in keywords):
                preferences[trait] = "Yes"
    return preferences

# Load and Process Data
bio_path = Path("bio.csv")
looking_for_path = Path("looking_for.csv")

for path in [bio_path, looking_for_path]:
    if not path.exists():
        print(f"Error: {path} not found. Please ensure the file exists.")
        sys.exit(1)

try:
    bio_data = pd.read_csv(bio_path)
    looking_for_data = pd.read_csv(looking_for_path)
except Exception as e:
    print(f"Error loading CSV files: {e}")
    sys.exit(1)

bio_data = bio_data.drop_duplicates(subset="uid", keep="first")
looking_for_data = looking_for_data.drop_duplicates(subset="uid", keep="first")

bio_data["interests"] = bio_data["bio"].apply(extract_interests)
bio_data["traits"] = bio_data["bio"].apply(extract_traits)
looking_for_data["preferences"] = looking_for_data["looking_for"].apply(extract_preferences)

try:
    merged_data = pd.merge(bio_data[["uid", "interests", "traits"]], 
                          looking_for_data[["uid", "preferences"]], 
                          on="uid", how="inner")
except Exception as e:
    print(f"Error merging data: {e}")
    sys.exit(1)

def expand_dict_column(df, column, prefix):
    expanded = pd.json_normalize(df[column])
    expanded.columns = [f"{prefix}_{col}" for col in expanded.columns]
    return pd.concat([df["uid"], expanded], axis=1)

interests_df = expand_dict_column(merged_data, "interests", "interest")
traits_df = expand_dict_column(merged_data, "traits", "trait")
preferences_df = expand_dict_column(merged_data, "preferences", "pref")

valid_traits = [f"trait_{trait}" for trait in traits_dict.keys()]
traits_df = traits_df[["uid"] + [col for col in traits_df.columns if col in valid_traits or col == "uid"]]

interests_df = interests_df.loc[:, ~interests_df.columns.duplicated()]
traits_df = traits_df.loc[:, ~traits_df.columns.duplicated()]
preferences_df = preferences_df.loc[:, ~preferences_df.columns.duplicated()]

try:
    final_data = pd.merge(interests_df, traits_df, on="uid", how="inner")
    final_data = pd.merge(final_data, preferences_df, on="uid", how="inner")
except Exception as e:
    print(f"Error during final merge: {e}")
    sys.exit(1)

numeric_cols = [col for col in final_data.columns if col.startswith("interest_") or col.startswith("trait_")]
final_data[numeric_cols] = final_data[numeric_cols].fillna(0).astype(int)
for col in numeric_cols:
    final_data[col] = final_data[col].clip(lower=0, upper=10)

final_data.to_csv("processed_data.csv", index=False)

# Matching Model with Feedback
data = pd.read_csv("processed_data.csv")
if data.empty:
    print("Error: processed_data.csv is empty.")
    sys.exit(1)

interest_cols = [col for col in data.columns if col.startswith("interest_")]
trait_cols = [col for col in data.columns if col.startswith("trait_")]
pref_cols = ["pref_shared_interests", "pref_humor", "pref_attractiveness", 
             "pref_intelligence", "pref_religion", "pref_ambition"]
feature_cols = interest_cols + trait_cols

numerical_features = data[feature_cols].fillna(0).values
numerical_similarity = cosine_similarity(numerical_features)
adjusted_similarity = numerical_similarity.copy()

# Feedback handling
feedback_data = pd.DataFrame(columns=["uid", "match_uid", "action"])
# Use real UIDs from data for simulation
sample_uids = data["uid"].head(3).tolist()
if len(sample_uids) < 3:
    print("Error: Not enough unique UIDs for feedback simulation.")
    sys.exit(1)
feedback_data = pd.concat([feedback_data, pd.DataFrame({
    "uid": [sample_uids[0], sample_uids[0], sample_uids[1]],
    "match_uid": [sample_uids[1], sample_uids[2], sample_uids[0]],
    "action": [1, -1, 1]  # 1 = accept, -1 = reject
})], ignore_index=True)

for _, row in feedback_data.iterrows():
    try:
        uid_idx = data.index[data["uid"] == row["uid"]].tolist()[0]
        match_idx = data.index[data["uid"] == row["match_uid"]].tolist()[0]
    except IndexError:
        print(f"Error: UID {row['uid']} or match_uid {row['match_uid']} not found in data.")
        continue
    if row["action"] == 1:
        adjusted_similarity[uid_idx, match_idx] *= 1.2
        adjusted_similarity[match_idx, uid_idx] *= 1.2
    elif row["action"] == -1:
        adjusted_similarity[uid_idx, match_idx] *= 0.8
        adjusted_similarity[match_idx, uid_idx] *= 0.8
    adjusted_similarity = np.clip(adjusted_similarity, a_min=0, a_max=1)

def compute_preference_score(row1, row2):
    score = 0
    total_checks = 0
    if row1["pref_shared_interests"] == "Yes" and row2["pref_shared_interests"] == "Yes":
        score += 1
    elif row1["pref_shared_interests"] == "Yes" or row2["pref_shared_interests"] == "Yes":
        score += 0.5
    total_checks += 1
    
    trait_pref_map = {
        "pref_humor": "trait_humor", "pref_attractiveness": "trait_attractiveness",
        "pref_intelligence": "trait_intelligence", "pref_ambition": "trait_ambition"
    }
    for pref_col, trait_col in trait_pref_map.items():
        if row1[pref_col] == "Yes":
            score += 1 if row2[trait_col] >= 7 else 0
            total_checks += 1
        if row2[pref_col] == "Yes":
            score += 1 if row1[trait_col] >= 7 else 0
            total_checks += 1
    
    religion1, religion2 = row1["pref_religion"], row2["pref_religion"]
    if pd.notna(religion1) and pd.notna(religion2):
        score += 1
    elif pd.isna(religion1) and pd.isna(religion2):
        score += 1
    total_checks += 1
    
    return score / total_checks if total_checks > 0 else 0

def combined_similarity(idx1, idx2, num_sim_matrix, df):
    num_score = num_sim_matrix[idx1, idx2]
    row1 = df.iloc[idx1]
    row2 = df.iloc[idx2]
    pref_score = compute_preference_score(row1, row2)
    return 0.7 * num_score + 0.3 * pref_score

def find_top_matches(df, sim_matrix, top_n=5):
    matches = {}
    for idx1 in range(len(df)):
        uid1 = df.iloc[idx1]["uid"]
        scores = []
        for idx2 in range(len(df)):
            if idx1 != idx2:
                uid2 = df.iloc[idx2]["uid"]
                score = combined_similarity(idx1, idx2, sim_matrix, df)
                scores.append((uid2, score))
        scores.sort(key=lambda x: x[1], reverse=True)
        matches[uid1] = scores[:top_n]
    return matches

# Execute matching
print("Initial Matches:")
top_matches = find_top_matches(data, adjusted_similarity)
for uid, match_list in top_matches.items():
    print(f"UID {uid}: {[f'UID {m[0]} ({m[1]:.3f})' for m in match_list]}")

new_feedback = pd.DataFrame({
    "uid": [sample_uids[0], sample_uids[1]],
    "match_uid": [sample_uids[2], sample_uids[0]],
    "action": [1, -1]
})


feedback_data = pd.concat([feedback_data, new_feedback], ignore_index=True)

for _, row in new_feedback.iterrows():
    try:
        uid_idx = data.index[data["uid"] == row["uid"]].tolist()[0]
        match_idx = data.index[data["uid"] == row["match_uid"]].tolist()[0]
    except IndexError:
        print(f"Error: UID {row['uid']} or match_uid {row['match_uid']} not found in data.")
        continue
    if row["action"] == 1:
        adjusted_similarity[uid_idx, match_idx] *= 1.2
        adjusted_similarity[match_idx, uid_idx] *= 1.2
    elif row["action"] == -1:
        adjusted_similarity[uid_idx, match_idx] *= 0.8
        adjusted_similarity[match_idx, uid_idx] *= 0.8
    adjusted_similarity = np.clip(adjusted_similarity, a_min=0, a_max=1)

print("\nUpdated Matches After Feedback:")
top_matches = find_top_matches(data, adjusted_similarity)
for uid, match_list in top_matches.items():
    print(f"UID {uid}: {[f'UID {m[0]} ({m[1]:.3f})' for m in match_list]}")

match_df = pd.DataFrame([
    {"uid": uid, "match_uid": match[0], "score": match[1], "rank": rank + 1}
    for uid, matches in top_matches.items()
    for rank, match in enumerate(matches)
])
match_df.to_csv("final_matches.csv", index=False)
print("\nFinal matches saved to final_matches.csv")