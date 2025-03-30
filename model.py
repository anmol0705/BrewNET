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

interests_dict = {
    "sports": ["sports", "hiking", "gym", "exercise", "yoga"],
    "dining": ["dining", "food", "restaurant", "meal"],
    "movies": ["movie", "tv", "film", "show"],
    "art": ["art", "museum", "theater", "concert"],
    "reading": ["book", "read", "literature", "novel", "poetry", "nonfiction"],
    "music": ["music", "concert", "symphony"],
    "clubbing": ["club", "dance", "nightlife"],
    "shopping": ["shopping", "retail"],
    "gaming": ["gaming", "game", "video game", "board game", "card game"],
    "fashion": ["fashion", "style", "clothing", "accessories", "trend"],
    "wellness": ["wellness", "meditation", "mindfulness", "mental health", "self-improvement"],
    "sustainability": ["sustainability", "environment", "eco-friendly", "green living", "climate"],
    "photography": ["photography", "videography", "content creation", "photo", "video"],
    "volunteering": ["volunteer", "community", "charity", "helping"],
    "finance": ["finance", "investing", "stocks", "crypto", "real estate", "money"],
    "science": ["science", "technology", "scientific", "tech", "discovery"],
    "pets": ["pet", "animal", "dog", "cat", "rescue"],
    "fitness": ["fitness", "exercise", "workout", "yoga", "gym"],
    "dance": ["dance", "performing arts", "theater", "performance"],
    "history": ["history", "culture", "historical", "tradition"],
    "languages": ["language", "linguistics", "grammar", "bilingual"],
    "podcasts": ["podcast", "audiobook"],
    "cars": ["car", "motorcycle", "vehicle", "riding", "restoration"],
    "diy": ["diy", "craft", "handmade", "home improvement", "upcycling"],
    "nutrition": ["nutrition", "health", "diet", "eating", "holistic"]
}

traits_dict = {
    "attractiveness": ["attractive", "looks", "beauty", "charming"],
    "intelligence": ["smart", "intelligent", "brain", "wise"],
    "humor": ["funny", "humor", "wit", "laugh"],
    "sincerity": ["sincere", "honest", "genuine", "authentic"],
    "ambition": ["ambitious", "driven", "goals"],
    "loyalty": ["loyal", "dependable", "count on", "reliable"],
    "curiosity": ["curious", "open-minded", "learning", "exploring", "perspective"],
    "supportiveness": ["supportive", "encouraging", "motivate", "helps"],
    "playfulness": ["playful", "humorous", "fun", "laugh", "joke"],
    "passion": ["passionate", "driven", "enthusiasm", "determination"],
    "kindness": ["kind", "compassionate", "empathy", "caring"],
    "trustworthiness": ["trustworthy", "keeps word", "honest", "dependable"],
    "independence": ["independent", "self-sufficient", "self-reliant"],
    "empowerment": ["empowering", "inspiring", "motivates", "lifts up"],
    "practicality": ["practical", "down-to-earth", "realistic"],
    "energy": ["energetic", "enthusiastic", "excitement", "adventure"],
    "thoughtfulness": ["thoughtful", "considerate", "feelings", "needs"],
    "assertiveness": ["assertive", "confident", "go after"],
    "creativity": ["creative", "innovative", "outside the box", "fresh ideas"],
    "spontaneity": ["spontaneous", "carefree", "moment", "chance"],
    "balance": ["balanced", "calm", "stability", "peace"]
}

preferences_dict = {
    "shared_interests": ["shared", "common", "similar", "hobby"],
    "religion": ["religion", "faith", "belief", "spiritual"],
    "loyalty_pref": ["loyal", "dependable", "count on"],
    "curiosity_pref": ["curious", "open-minded", "learning"],
    "supportiveness_pref": ["supportive", "encouraging", "motivate"],
    "playfulness_pref": ["playful", "humorous", "fun"],
    "passion_pref": ["passionate", "driven", "enthusiasm"],
    "kindness_pref": ["kind", "compassionate", "caring"],
    "trustworthiness_pref": ["trustworthy", "honest", "dependable"],
    "independence_pref": ["independent", "self-sufficient"],
    "empowerment_pref": ["empowering", "inspiring", "motivates"],
    "practicality_pref": ["practical", "down-to-earth"],
    "energy_pref": ["energetic", "enthusiastic", "excitement"],
    "thoughtfulness_pref": ["thoughtful", "considerate"],
    "assertiveness_pref": ["assertive", "confident"],
    "creativity_pref": ["creative", "innovative"],
    "spontaneity_pref": ["spontaneous", "carefree"],
    "balance_pref": ["balanced", "calm", "stability"]
}

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
    preferences = {
        # Original preferences
        "shared_interests": pd.NA,
        "religion": pd.NA,
        
        # Personality preferences (note we're using the same names as in traits_dict)
        "humor": pd.NA,
        "loyalty": pd.NA,
        "curiosity": pd.NA,
        "supportiveness": pd.NA,
        "playfulness": pd.NA,
        "passion": pd.NA,
        "kindness": pd.NA,
        "trustworthiness": pd.NA,
        "independence": pd.NA,
        "empowerment": pd.NA,
        "practicality": pd.NA,
        "energy": pd.NA,
        "thoughtfulness": pd.NA,
        "assertiveness": pd.NA,
        "creativity": pd.NA,
        "spontaneity": pd.NA,
        "balance": pd.NA
    }
    
    text_lower = str(text).lower()
    
    # Check for shared interests and religion first
    if any(keyword in text_lower for keyword in preferences_dict["shared_interests"]):
        preferences["shared_interests"] = "Yes"
    if any(keyword in text_lower for keyword in preferences_dict["religion"]):
        preferences["religion"] = "Religious"
    
    # Check for personality preferences
    for sentence in doc.sents:
        sent_text = sentence.text.lower()
        for trait, keywords in preferences_dict.items():
            if trait not in ["shared_interests", "religion"]:
                if any(keyword in sent_text for keyword in keywords):
                    # Use the base trait name without _pref suffix
                    pref_name = trait.replace("_pref", "")
                    preferences[pref_name] = "Yes"
    
    return preferences

# Load and Process Data
bio_path = Path("C:/Users/risha/Downloads/bio.csv")
looking_for_path = Path("C:/Users/risha/Downloads/looking_for.csv")

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
    
    # Shared interests check
    if row1["pref_shared_interests"] == "Yes" and row2["pref_shared_interests"] == "Yes":
        score += 1
    elif row1["pref_shared_interests"] == "Yes" or row2["pref_shared_interests"] == "Yes":
        score += 0.5
    total_checks += 1
    
    # Religion compatibility
    religion1, religion2 = row1["pref_religion"], row2["pref_religion"]
    if pd.notna(religion1) and pd.notna(religion2):
        score += 1
    elif pd.isna(religion1) and pd.isna(religion2):
        score += 1
    total_checks += 1
    
    # Personality preference checks
    personality_traits = [
        "humor", "loyalty", "curiosity", "supportiveness", "playfulness",
        "passion", "kindness", "trustworthiness", "independence",
        "empowerment", "practicality", "energy", "thoughtfulness",
        "assertiveness", "creativity", "spontaneity", "balance"
    ]
    
    for trait in personality_traits:
        pref_col = f"pref_{trait}"
        trait_col = f"trait_{trait}"
        
        # Only check if the preference column exists
        if pref_col in row1 and pref_col in row2:
            # Check if user1 wants this trait in user2
            if row1[pref_col] == "Yes":
                # Use get() with default value 0 in case trait_col doesn't exist
                trait_score = row2.get(trait_col, 0)
                score += 1 if trait_score >= 7 else 0
                total_checks += 1
            
            # Check if user2 wants this trait in user1
            if row2[pref_col] == "Yes":
                trait_score = row1.get(trait_col, 0)
                score += 1 if trait_score >= 7 else 0
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
