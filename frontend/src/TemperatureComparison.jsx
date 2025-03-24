import React, { useState, useEffect } from 'react';

function TemperatureComparison() {
    const [prompt, setPrompt] = useState('');
    const [responses, setResponses] = useState({});
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [selectedDeployment, setSelectedDeployment] = useState('openAI');
    const temperatures = [0, 0.5, 1];
    const [currentTemperatureIndex, setCurrentTemperatureIndex] = useState(0);

    const handlePromptChange = (event) => {
        setPrompt(event.target.value);
        setErrorMessage('');
    };

    const handleDeploymentChange = (event) => {
        setSelectedDeployment(event.target.value);
    };

    const handleCompare = async () => {
        if (prompt.trim() === '') {
            setErrorMessage('Prompt cannot be empty.');
            return;
        }

        setIsLoading(true);
        setResponses({}); // Clear previous responses
        setCurrentTemperatureIndex(0); // Reset the index
    };

    const handleKeyDown = (event) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault(); // Prevent a new line in the textarea
            handleCompare();
        }
    };

    useEffect(() => {
        const fetchNextTemperature = async () => {
            if (currentTemperatureIndex >= temperatures.length) {
                setIsLoading(false);
                return; // All temperatures processed
            }

            const temperature = temperatures[currentTemperatureIndex];
            try {
                const response = await fetch('/api/chat', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        input: prompt,
                        temperature: temperature,
                        deployment: selectedDeployment,
                    }),
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                setResponses((prevResponses) => ({
                    ...prevResponses,
                    [temperature]: data.response,
                }));
                setCurrentTemperatureIndex((prevIndex) => prevIndex + 1);
            } catch (error) {
                console.error(`Error comparing temperature ${temperature}:`, error);
                setErrorMessage(`Error occurred with temperature ${temperature}: ${error.message}`);
                setIsLoading(false);
            }
        };

        if (isLoading && prompt) {
            fetchNextTemperature();
        }
    }, [isLoading, currentTemperatureIndex, prompt, selectedDeployment, temperatures]);

    return (
        <div className="comparison-container">
            <h2>Compare Temperatures</h2>
            <div className="comparison-input">
                <textarea
                    rows="1"
                    className="comparison-input-textarea"
                    value={prompt}
                    onChange={handlePromptChange}
                    placeholder="Enter your prompt"
                    onKeyDown={handleKeyDown}
                />
                <button onClick={handleCompare}>Compare</button>
            </div>
            <div className="control-group">
                <label htmlFor="deployment">Deployment:</label>
                <select id="deployment" value={selectedDeployment} onChange={handleDeploymentChange}>
                    <option value="openAI">OpenAI</option>
                    <option value="mistral">Mistral</option>
                    <option value="deepseek">DeepSeek</option>
                </select>
            </div>
            {errorMessage && <div className="error-message">{errorMessage}</div>}
            {isLoading && <div className="loading">Loading...</div>}
            <div className="responses-container">
                {temperatures.map((temperature) => (
                    <div key={temperature} className="response-box">
                        <h3>Temperature: {temperature}</h3>
                        <div className="response-content">
                            {responses[temperature] ? (
                                <p>{responses[temperature]}</p>
                            ) : (
                                <p>No response yet.</p>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default TemperatureComparison;