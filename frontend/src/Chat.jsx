import React, { useState, useRef } from 'react';

function Chat() {
    const [message, setMessage] = useState('');
    const [chatLog, setChatLog] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [temperature, setTemperature] = useState(0.7);
    const [selectedDeployment, setSelectedDeployment] = useState('openAI');
    const [selectedFile, setSelectedFile] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadStatusMessage, setUploadStatusMessage] = useState('');
    const [uploadErrorMessage, setUploadErrorMessage] = useState('');
    const [activeFilename, setActiveFilename] = useState(null);
    const [isFileUploaded, setIsFileUploaded] = useState(false);
    const fileInputRef = useRef(null);

    const handleInputChange = (event) => {
        setMessage(event.target.value);
        setErrorMessage('');t
    };

    const handleTemperatureChange = (event) => {
        setTemperature(parseFloat(event.target.value));
    };

    const handleDeploymentChange = (event) => {
        setSelectedDeployment(event.target.value);
    };

    const handleKeyDown = (event) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            handleSendMessage();
        }
    };

    // --- File Input Change Handler ---
    const handleFileChange = (event) => {
        const file = event.target.files[0];
        if (file) {
            if (file.type === 'application/pdf' || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') {
                setSelectedFile(file);
                setUploadErrorMessage('');
                setUploadStatusMessage('');
            } else {
                setUploadErrorMessage('Invalid file type. Please select a PDF or DOCX file.');
                setSelectedFile(null);
                if (fileInputRef.current) {
                    fileInputRef.current.value = "";
                }
            }
        } else {
            setSelectedFile(null);
        }
    };

    // --- File Upload Handler ---
    const handleUpload = async () => {
        if (!selectedFile) {
            setUploadErrorMessage('Please select a file first.');
            return;
        }

        setIsUploading(true);
        setUploadErrorMessage('');
        setUploadStatusMessage('Uploading ${selectedFile.name}...');
        setActiveFilename(null);

        const formData = new FormData();
        formData.append('file', selectedFile);

        try {

            const response = await fetch('/rag/upload', {
                method: 'POST',
                body: formData,
            });

            const resultText = await response.text();

            if (!response.ok) {
                throw new Error(resultText || `Upload failed with status: ${response.status}`);
            }


            setUploadStatusMessage(resultText);
            setActiveFilename(selectedFile.name);
            setChatLog(prev => [...prev, { role: 'system', content: `Document '${selectedFile.name}' is now active for Q&A.` }]);
            setSelectedFile(null);


        } catch (error) {
            console.error('Error uploading file:', error);
            setUploadErrorMessage(`Upload failed: ${error.message}`);
            setUploadStatusMessage('');
            setActiveFilename(null);
             setSelectedFile(null);

        } finally {
            setIsUploading(false);
            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }
        }
    };

const clearActiveFile = () => {
        if (activeFilename) {
            setChatLog(prev => [...prev, { role: 'system', content: `Document context '${activeFilename}' cleared.` }]);
            setActiveFilename(null);
            setUploadStatusMessage('');
        }
    };

    const handleSendMessage = async () => {
        const currentMessage = message.trim();
        if (currentMessage === '') {
            setErrorMessage("Input prompt cannot be empty.");
            return;
        }

        setChatLog(prevChatLog => [...prevChatLog, { role: 'user', content: currentMessage }]);
        setMessage('');
        setIsLoading(true);
        setErrorMessage('');


        const endpoint = '/rag/query';

        const requestBody = {
                    input: currentMessage,
                    temperature: temperature,
                    deployment: selectedDeployment,
                    sourceFilename: activeFilename
                };

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody),
            });


            if (!response.ok) {
                let errorData;
                try {
                    errorData = await response.json();
                } catch (parseError) {
                    errorData = { error: response.statusText };
                }

                throw new Error(errorData?.error || errorData?.message || `HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

             if (data.error) {
                 throw new Error(data.error);
             }

            setChatLog(prevChatLog => [...prevChatLog, { role: 'assistant', content: data.response }]);

        } catch (error) {
            console.error('Error sending message:', error);
            setChatLog(prevChatLog => [...prevChatLog, { role: 'error', content: "Error occurred: " + error.message }]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="chat-container">

            {/* --- File Upload Section --- */}
            <div className="file-upload-section">
                <h3>Upload Document for RAG</h3>
                <input
                    type="file"
                    ref={fileInputRef} // Add ref
                    accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" // Be more specific
                    onChange={handleFileChange}
                    disabled={isUploading}
                />
                <button onClick={handleUpload} disabled={!selectedFile || isUploading}>
                    {isUploading ? 'Uploading...' : 'Upload & Activate'}
                </button>
                {uploadStatusMessage && !uploadErrorMessage && <div className="upload-status success">{uploadStatusMessage}</div>}
                {uploadErrorMessage && <div className="upload-status error">{uploadErrorMessage}</div>}
                {activeFilename && (
                                    <div className="active-file-indicator">
                                        <span>Active Document: <strong>{activeFilename}</strong></span>
                                        <button onClick={clearActiveFile} title="Clear document context" className="clear-button">
                                            ✖
                                        </button>
                                    </div>
                                )}
            </div>
            {/* --- End File Upload Section --- */}

            <hr className="section-separator" />

            <div className="chat-log">
                {chatLog.map((msg, index) => (
                    <div key={index} className={`message ${msg.role}`}>
                       {/* Simple paragraph rendering, consider markdown rendering later */}
                       <p> {msg.content} </p>
                    </div>
                ))}
                {isLoading && <div className="message loading">Loading...</div>}
            </div>

            {errorMessage && <div className="error-message">{errorMessage}</div>}

            <div className="chat-input">
                <textarea
                    value={message}
                    onChange={handleInputChange}
                    onKeyDown={handleKeyDown}
                    placeholder={activeFilename ? `Ask about ${activeFilename}...` : "Enter your message or upload a document"}
                    disabled={isLoading}
                    rows={3}
                />
                <button onClick={handleSendMessage} disabled={isLoading || message.trim() === ''}>
                    {isLoading ? 'Thinking...' : 'Send'}
                </button>
            </div>

            <div className="controls-container">
                <div className="control-group">
                    <label htmlFor="temperature">Temperature:</label>
                    <input
                        type="number"
                        id="temperature"
                        name="temperature"
                        min="0"
                        max="1"
                        step="0.1"
                        value={temperature}
                        onChange={handleTemperatureChange}
                    />
                </div>
                <div className="control-group">
                    <label htmlFor="deployment">Deployment:</label>
                    <select id="deployment" value={selectedDeployment} onChange={handleDeploymentChange}>
                        <option value="openAI">OpenAI</option>
                        <option value="mistral">Mistral</option>
                        <option value="deepseek">DeepSeek</option>
                    </select>
                </div>
            </div>
        </div>
    );
}

export default Chat;