import React, { useState, useEffect } from 'react';

function Comparison() {
    const [prompt, setPrompt] = useState('');
    const [responses, setResponses] = useState({});
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const deployments = ['openAI', 'mistral', 'deepseek'];
    const [currentDeploymentIndex, setCurrentDeploymentIndex] = useState(0);

    const handlePromptChange = (event) => {
        setPrompt(event.target.value);
        setErrorMessage('');
    };

    const handleCompare = async () => {
        if (prompt.trim() === '') {
            setErrorMessage('Prompt cannot be empty.');
            return;
        }

        setIsLoading(true);
        setResponses({}); // Clear previous responses
        setCurrentDeploymentIndex(0); // Reset the index
    };

    const handleKeyDown = (event) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault(); // Prevent a new line in the textarea
            handleCompare();
        }
    };

    useEffect(() => {
        const fetchNextDeployment = async () => {
            if (currentDeploymentIndex >= deployments.length) {
                setIsLoading(false);
                return; // All deployments processed
            }

            const deployment = deployments[currentDeploymentIndex];
            try {
                const response = await fetch('/api/chat', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        input: prompt,
                        temperature: 0.7,
                        deployment: deployment,
                    }),
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                setResponses((prevResponses) => ({
                    ...prevResponses,
                    [deployment]: data.response,
                }));
                setCurrentDeploymentIndex((prevIndex) => prevIndex + 1);
            } catch (error) {
                console.error(`Error comparing ${deployment}:`, error);
                setErrorMessage(`Error occurred with ${deployment}: ${error.message}`);
                setIsLoading(false);
            }
        };

        if (isLoading && prompt) {
            fetchNextDeployment();
        }
    }, [isLoading, currentDeploymentIndex, prompt, deployments]);

    return (
        <div className="comparison-container">
            <h2>Compare Deployments</h2>
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
            {errorMessage && <div className="error-message">{errorMessage}</div>}
            {isLoading && <div className="loading">Loading...</div>}
            <div className="responses-container">
                {deployments.map((deployment) => (
                    <div key={deployment} className="response-box">
                        <h3>{deployment}</h3>
                        <div className="response-content">
                            {responses[deployment] ? (
                                <p>{responses[deployment]}</p>
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

export default Comparison;