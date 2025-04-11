# Gen AI Chatbot Application - Setup and Testing Guide

This document provides instructions on how to run the Gen AI Chatbot application, which includes a Spring Boot backend and a React frontend. It also guides you on testing the chat history feature.

## Prerequisites

Before you begin, ensure you have the following installed:

*   **Java Development Kit (JDK) 17 or higher:** Required for running the Spring Boot backend.
*   **Apache Maven:** Used to build and manage the Spring Boot project.
*   **Node.js (with npm):** Required for running the React frontend. It's recommended to use the latest LTS version of Node.js.
*   **OpenAI Account and API Key:** You need an active OpenAI account and a valid API key to use the AI model.
*   **Text Editor/IDE:** (e.g., VS Code, IntelliJ IDEA) for editing code.
*   **Docker & Docker Compose:** Installed and running to manage the Qdrant vector database.

## Backend (Spring Boot) Setup and Run

1.  **Navigate to the GenAiTrainingApplication Class:**

    ```bash
    cd ${project-root-directory}/gen_ai_training/src/main/java/com/epam/training/gen/ai
    ```

2. **Build the Backend:**

    ```bash
    mvn clean install
    ```

    This command cleans the project, compiles the code, runs tests, and packages it into a JAR file.

3. **Run the Spring Boot Application:**

    ```bash
    mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DOPEN_AI_KEY=YOUR_OPENAI_API_KEY -DOPEN_AI_ENDPOINT=your_endpoint_url"
    ```
     *   Replace `YOUR_OPENAI_API_KEY`, `your_endpoint_url` with your actual OpenAI API key and endpoint.
     *   This command starts the Spring Boot application on port 8080 (by default) and sets the necessary environment variables. You should see logs in the console indicating that the application has started successfully.
     *   If you are using a different model, make sure you configure it properly.
           This starts the Spring Boot application on port 8080 (by default). You should see logs in the console indicating that the application has started successfully.

## Frontend (React) Setup and Run

1.  **Navigate to the Frontend Directory:**

    ```bash
    cd ${project-root-directory}/gen_ai_training/frontend
    ```

2.  **Install Dependencies:**

    ```bash
    npm install
    ```

    This command installs all the necessary Node.js packages listed in the `package.json` file.

3.  **Start the Frontend:**

    ```bash
    npm run dev
    ```

    This starts the React development server on `http://localhost:5173`.

## Qdrant Setup and Run

1. **Start Docker Engine**
    
2. **Start Qdrant Database:**
   Use Docker Compose to start the Qdrant service defined in `docker-compose.yml`:
    ```
   bash docker-compose up -d
   ```

## Accessing the Application

Once both the backend and frontend are running, access the application in your web browser at: http://localhost:5173


# Module 2

## Testing the Chat History Feature

The application maintains a chat history within the `chatHistory` instance (in `ChatBotService`) for the duration of your session. To test this:

1.  **Start a Conversation:**
    *   Go to `http://localhost:5173` in your browser.
    *   Type a prompt in the input box and click "Send."
    *   The AI will respond, and the conversation will be displayed in the chat log.

2.  **Continue the Conversation:**
    *   Ask the AI follow-up questions or provide more context in subsequent prompts.

3.  **Verify History:**
    *   As you send subsequent messages, the chat log should show the entire conversation history up to that point.
    *   **Important:** Closing/Refreshing the browser or restarting the frontend/backend will clear the chat history.

## Example Prompts for Testing

Here are some example prompts to test the chat history and context understanding:

1.  **Initial Prompt:**

    ```
    What is the capital of France?
    ```

    **Expected:** The AI should respond with "Paris" (or a similar answer).

2.  **Follow-up Prompt (Referencing Previous Response):**

    ```
    What is the population of this city?
    ```

    **Expected:** The AI should understand that "this city" refers to Paris and provide its population. The previous response should be taken into account.

3.  **Another follow-up Prompt:**
    ```
    Tell me more about the history of this city.
    ```
    **Expected:** The AI should know we are talking about Paris.

4.  **Out of Context:**

    ```
    What is the capital of Argentina?
    ```
    **Expected:** The bot should answer the current question.

5. **After the previous prompt, ask:**
    ```
     What is the population of this city?
    ```
   **Expected**: The bot should understand that now we are talking about Argentina's capital and not France's one.

6.  **Check Error Handling:**

    *   **Empty Prompt:**
        ```
        Send an empty prompt.
        ```
        **Expected:** The frontend will display an error: "Input prompt cannot be empty."

# Module 3

### Selecting a Deployment & Temperature in Chat tab

You can choose which AI model deployment to use for your chat interactions:

1.  **Deployment Dropdown:**
    *   In the "Chat" tab, you'll find a "Deployment" dropdown menu.
    *   The available options are: `OpenAI`, `Mistral`, and `DeepSeek`.
2.  **Select a Deployment:**
    *   Choose the deployment you want to use from the dropdown.
    *   All subsequent messages will be processed by the selected deployment.
3.  **Temperature Input:**
    *   In the "Chat" tab, you'll find a "Temperature" input field.
    *   The temperature value ranges from `0` to `1`.
4.  **Adjust the Value:**
    *   Enter a value between `0` and `1` (e.g., `0.2`, `0.7`, `1.0`).
    *   **Lower Temperature (closer to 0):** Results in more deterministic and focused responses.
    *   **Higher Temperature (closer to 1):** Results in more creative and varied responses.


### Using the Deployment Comparison Tab

The "Comparison" tab allows you to compare the responses of different deployments side-by-side:

1.  **Navigate to the Comparison Tab:**
    *   Click on the "Comparison" tab in the application.
2.  **Enter a Prompt:**
    *   Type your prompt in the input field.
3.  **Click "Compare":**
    *   Click the "Compare" button.
4.  **View the Results:**
    *   The application will sequentially fetch responses from each deployment (`OpenAI`, `Mistral`, `DeepSeek`).
    *   The responses will be displayed in separate boxes, allowing you to compare them.
    *   The responses will be displayed one by one.

### Using the Temperature Comparison Tab

The "Temperature Comparison" tab allows you to compare the responses of a single selected deployment across different temperature settings (0, 0.5, and 1):

1.  **Navigate to the Temperature Comparison Tab:**
    *   Click on the "Temperature Comparison" tab in the application.

2.  **Select a Deployment:**
    *   Use the "Deployment" dropdown to choose the AI model you want to test (e.g., `OpenAI`, `Mistral`, `DeepSeek`).

3.  **Enter a Prompt:**
    *   Type your prompt in the input field.

4.  **Click "Compare":**
    *   Click the "Compare" button.

5.  **View the Results:**
    *   The application will sequentially fetch responses from the selected deployment for each temperature setting (0, 0.5, and 1).
    *   The responses will be displayed in separate boxes, labeled with their respective temperatures.
    *   The responses will be displayed one by one.
    *   You can then compare how the temperature setting affects the output of the chosen AI model.

**Understanding Temperature's Impact:**

*   **Temperature 0:** This setting produces the most deterministic and focused responses. The AI will consistently choose the most likely next word or phrase.
*   **Temperature 0.5:** This setting introduces some randomness, allowing for more variation in the responses while still maintaining a degree of focus.
*   **Temperature 1:** This setting produces the most creative and varied responses. The AI is more likely to choose less probable words or phrases, leading to more surprising and diverse outputs.

**Use Cases:**

*   **Experimentation:** Use this tab to understand how temperature affects the output of different AI models.
*   **Fine-Tuning:** Determine the optimal temperature setting for your specific use case.
*   **Creativity vs. Accuracy:** Explore the trade-off between creative, varied responses and more focused, deterministic responses.

# Module 4

Please use the chat tab in the frontend application for this module.

### Testing the Age Calculator Plugin
* Prompt:
    ```
    Calculate my age if I was born on 1990-05-15.
    ```
  **Expected:** The AI should respond with the calculated age (e.g., "33" or "34", depending on the current date).
* Prompt:
    ```
    Calculate my age if I was born on 01 January 2000.
   ```
  **Expected:** The AI should respond with the calculated age.

### Testing the Weather Plugin:
* Prompt:
    ```
    What is the current weather in London?
    ```
  **Expected:** The AI should respond with the current weather in London (e.g., "The current weather in London is: Cloudy, 15°C.").

# Module 5

### Testing the Embeddings Endpoints
1. Create and Store an Embedding
    ```
   curl -X POST -H "Content-Type: application/json"
    -d '{"text": "The quick brown fox jumps over the lazy dog."}'
    http://localhost:8080/embedding/build-and-store
   ```
   ```
   curl -X POST -H "Content-Type: application/json"
    -d '{"text": "A fast, dark-colored mammal leaps above a sleepy canine."}'
    http://localhost:8080/embedding/build-and-store
   ```
   Check the application logs and the response from `curl` to ensure the requests were successful.

2. Search for Similar Embeddings
    ```
   curl -X POST -H "Content-Type: application/json"
    -d '{"text": "agile fox", "limit": 2}'
    http://localhost:8080/embedding/search
   ```
3. Create an Embedding from a text
    ```
   curl -X POST -H "Content-Type: application/json"
    -d '{"text": "The quick brown fox jumps over the lazy dog."}'
    http://localhost:8080/embedding/build
   ```
   Check the application logs and the response from `curl` to ensure the requests were successful.

# Module 6

## Testing the RAG (Retrieval-Augmented Generation) Feature

This feature allows the chatbot to answer questions based on the content of documents you upload. The system extracts text, creates embeddings, stores them in Qdrant, and retrieves relevant chunks to augment the AI's response generation.

**Testing Steps:**

1.  **Navigate to the Chat Tab:**
    *   Ensure you are on the main **Chat** tab in the frontend application (`http://localhost:5173`).

2.  **Upload a Document:**
    *   Locate the file upload section within the Chat tab (look for a button like "Upload File" or an upload icon).
    *   Click the upload button and select a document (e.g., PDF, DOCX) to upload.
    *   **Suggestion:** You can use the sample PDF files located in the backend project's test resources directory: `src/test/resources/data/`.

3.  **Wait for Processing:**
    *   After selecting the file, the frontend will send it to the backend.
    *   The backend will then:
        *   Extract text from the document.
        *   Chunk the text.
        *   Generate embeddings for each chunk using the configured OpenAI model.
        *   Store the chunks and their embeddings in the Qdrant vector database.
    *   This process might take a few moments, especially for larger files. Look for a confirmation message in the UI (e.g., "File uploaded and processed successfully") or check the backend console logs for progress information (e.g., "Stored embedding for chunk...", "Embedding process completed").

4.  **Ask Questions Based on the Document:**
    *   Once the document is processed and embeddings are stored, you can ask questions specifically related to the content of the uploaded file in the chat input box.

**Example Prompts for RAG Testing:**

*(Note: The effectiveness of these prompts depends heavily on the actual content of the PDF/DOCX file you upload. Use questions relevant to your chosen test document.)*

*   **Examples:**
  * PerkPlus.pdf -> What is covered under the PerksPlus?
  * Benefits_Options.pdf -> what is Northwind Health Plus?

**Expected Behavior:**

The chatbot should provide answers that are clearly derived from the content of the document you uploaded. It should retrieve relevant text chunks from Qdrant and use them to formulate an informed response, rather than relying solely on its general pre-trained knowledge. If you ask about something *only* present in the document, the RAG feature should enable the bot to answer correctly.

## Important Notes

*   **CORS:** The backend is configured to allow requests from `http://localhost:5173` (see `ChatBotController.java`). If you change the frontend's port, update the `@CrossOrigin` annotation accordingly.
*   **API Keys:** Double-check that your OpenAI API key and deployment name are correctly configured in `application.properties`.
*   **Error Handling:** Both the frontend and backend have basic error handling, but more sophisticated error handling could be implemented.
* **OpenAI deployment name:** If you are using a different model, make sure you configure it properly.

## Troubleshooting

*   If you encounter issues, carefully check the console output of both the Spring Boot application and the React development server for error messages.
*   Ensure that the backend is running *before* starting the frontend.
*   Verify your API keys and deployment names if you encounter issues with the AI's responses.

This comprehensive guide should help you set up and run the application smoothly. Please let me know if you have any questions!