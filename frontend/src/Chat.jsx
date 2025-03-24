import React, { useState } from 'react';
function Chat() {
    const [message, setMessage] = useState('');
    const [chatLog, setChatLog] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [temperature, setTemperature] = useState(0.7);
      const [selectedDeployment, setSelectedDeployment] = useState('openAI');

    const handleInputChange = (event) => {
        setMessage(event.target.value);
        setErrorMessage('');
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

    const handleSendMessage = async () => {
        if (message.trim() === ''){
            setErrorMessage("Input prompt cannot be empty.");
            return;
        }
        // Add user message to chat log
        setChatLog([...chatLog, { role: 'user', content: message }]);
        setMessage(''); // Clear the input
        setIsLoading(true);
        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    input: message,
                    temperature: temperature,
                    deployment: selectedDeployment
                }),
            });
            if (!response.ok) {
              const errorData = await response.json();
              throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            // Add assistant response to chat log
            setChatLog([...chatLog, { role: 'user', content: message }, { role: 'assistant', content: data.response }]);
        } catch (error) {
          console.error('Error sending message:', error);
          setChatLog([...chatLog, { role: 'user', content: message }, { role: 'error', content: "Error occurred: " + error.message }]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="chat-container">
            <div className="chat-log">
                {chatLog.map((msg, index) => (
                    <div key={index} className={`message ${msg.role}`}>
                       <p> {msg.content} </p>
                    </div>
                ))}
                {isLoading && <div className="message loading">Loading...</div>}
            </div>
            {errorMessage && <div className="error-message">{errorMessage}</div>}
            <div className="chat-input">
                <input
                    type="text"
                    value={message}
                    onChange={handleInputChange}
                     onKeyDown={handleKeyDown}
                    placeholder="Enter your message"
                />
                <button onClick={handleSendMessage}>Send</button>
            </div>
            <div className="controls-container"> {/* Renamed outer div */}
                <div className="control-group"> {/* Corrected class name */}
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
                <div className="control-group"> {/* Corrected class name */}
                    <label htmlFor="deployment">Deployment:</label>
                    <select id="deployment" value={selectedDeployment}
                        onChange={handleDeploymentChange}>
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