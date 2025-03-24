# BrewNET

Welcome to BrewNET, a slick compatibility model that turns raw user data into precise matches. Built for a hackathon, this beast uses NLP, cosine similarity, and real-time feedback to pair people based on interests, traits, and preferences. Whether it’s sports buffs, ambitious pros, or religious souls, this engine’s got it covered.

# What It Does
Inputs: User bios (bio.csv) and preferences (looking_for.csv).
Outputs: Top 5 matches per user, saved to final_matches.csv.

Extracts interests (e.g., sports: 7/10) and traits (e.g., humor: 6/10) from bios.
Tags preferences (e.g., "Yes" for humor, "Religious" for faith) from what users want.
Matches via 70% numerical similarity + 30% preference scoring, refined by feedback (+20% accept, -20% reject).
Use Case: Powering a swipe-first, network-smart app for friendships, mentorships, or vibes.

# Features
NLP-Powered: SpaCy and VADER sentiment analysis for feature extraction.
Dynamic Matching: Cosine similarity with real-time feedback adjustments.
Scalable: Ready to plug into a full-stack social platform.

# Workflow
Data Input: Loads user bios and preferences.
Feature Extraction: NLP turns text into scores (0-10) and tags (Yes/Religious).
Structured Data: Builds processed_data.csv with interests, traits, prefs.
Matching Engine: Cosine similarity + preference scoring = top 5 matches.
Feedback Loop: User actions tweak scores for smarter pairing.

# Built With
Python: 3.12
SpaCy: 3.7+ 
NLTK/VADER: Sentiment scoring
Scikit-learn: Cosine similarity
