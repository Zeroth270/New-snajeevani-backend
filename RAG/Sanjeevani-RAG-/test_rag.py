import os
import time
import requests

RAG_URL = "http://localhost:8000"

def test_pipeline():
    print("Step 1: Checking RAG Service health...")
    try:
        r = requests.get(f"{RAG_URL}/health")
        print(f"Health Response: {r.status_code} - {r.json()}\n")
    except Exception as e:
        print(f"Error connecting to RAG service: {e}")
        print("Make sure you started the uvicorn server on port 8000 first!")
        return

    # Check if a test file exists, otherwise build a text document for the test run
    test_file = "sample_test.txt"
    with open(test_file, "w", encoding="utf-8") as f:
        f.write(
            "Sanjeevani AI is a breakthrough named entity recognition and RAG platform. "
            "It extracts chemical structures like 2-acetoxybenzoic acid and 1-methylimidazole from research papers. "
            "The system was developed under Section 31 grace period guidelines to protect university IP."
        )
    print(f"Created temporary text file for testing: {test_file}")

    print(f"Step 2: Uploading and Indexing '{test_file}'...")
    start = time.time()
    with open(test_file, "rb") as f:
        files = {"file": f}
        data = {"title": "Sanjeevani Test Document"}
        r = requests.post(f"{RAG_URL}/documents/index", files=files, data=data)
    
    if r.status_code != 200:
        print(f"Failed to index document: {r.status_code} - {r.text}")
        return
        
    doc_data = r.json()
    doc_id = doc_data.get("document_id")
    print(f"Indexed successfully in {time.time() - start:.2f}s! Document ID: {doc_id}")
    print(f"Response: {doc_data}\n")

    print("Step 3: Querying the index (Chat Q&A)...")
    query_payload = {
        "question": "What chemical structures does Sanjeevani AI extract, and under what guidelines was it developed?",
        "top_k": 5,
        "similarity_threshold": 0.5,
        "use_mmr": True
    }
    
    r = requests.post(f"{RAG_URL}/chat", json=query_payload)
    if r.status_code != 200:
        print(f"Failed to query chat: {r.status_code} - {r.text}")
        return
        
    chat_response = r.json()
    print("--------------------------------------------------")
    print("ANSWER:")
    print(chat_response.get("answer"))
    print("--------------------------------------------------")
    print("CITATIONS:")
    for citation in chat_response.get("citations", []):
        print(f"- {citation.get('title')} (Similarity: {citation.get('similarity_score'):.3f})")
        print(f"  Snippet: {citation.get('chunk_text')[:120]}...")
    print("--------------------------------------------------")

    # Clean up temporary test file
    try:
        os.remove(test_file)
    except:
        pass

if __name__ == "__main__":
    test_pipeline()
