# Gen AI Chatbot Application - Setup and Testing Guide

This document provides instructions on how to run the Gen AI Chatbot application, which includes a Spring Boot backend and a React frontend. It also guides you on testing the chat history feature.

## Prerequisites

Before you begin, ensure you have the following installed:

*   **Java Development Kit (JDK) 17 or higher:** Required for running the Spring Boot backend.
*   **Apache Maven:** Used to build and manage the Spring Boot project.
*   **Node.js (with npm):** Required for running the React frontend. It's recommended to use the latest LTS version of Node.js.
*   **OpenAI Account and API Key:** You need an active OpenAI account and a valid API key to use the AI model.
*   **Text Editor/IDE:** (e.g., VS Code, IntelliJ IDEA) for editing code.

## Backend (Spring Boot) Setup and Run

1.  **Navigate to the GenAiTrainingApplication Class:**

    ```bash
    cd ${project-root-directory}/gen_ai_training/src/main/java/com/epam/training/gen/ai
    ```

2.  **Configure Env variables:**

    *   Edit Run configurations.
    *   Add your OpenAI API key, endpoint and model deployment name as environment variables:

        ```properties
        # OpenAI
        OPEN_AI_KEY=YOUR_OPENAI_API_KEY
        OPEN_AI_ENDPOINT= your endpoint url
        OPEN_AI_DEPLOYMENT_NAME=gpt-4 # or your deployment name
        ```

3.  **Build the Backend:**

    ```bash
    mvn clean install
    ```

    This command cleans the project, compiles the code, runs tests, and packages it into a JAR file.

4.  **Run the Spring Boot Application:**

    ```bash
    mvn spring-boot:run
    ```

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