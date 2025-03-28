import React, { useState } from 'react';
import Chat from './chat';
import Comparison from './Comparison';
import TemperatureComparison from './TemperatureComparison';
import './App.css';

function App() {
    const [activeTab, setActiveTab] = useState('chat');

    return (
        <div className="App">
            <div className="tabs">
                <button
                    className={activeTab === 'chat' ? 'active' : ''}
                    onClick={() => setActiveTab('chat')}
                >
                    Chat
                </button>
                <button
                    className={activeTab === 'comparison' ? 'active' : ''}
                    onClick={() => setActiveTab('comparison')}
                >
                    Deployment Comparison
                </button>
                <button
                    className={activeTab === 'temperatureComparison' ? 'active' : ''}
                    onClick={() => setActiveTab('temperatureComparison')}
                >
                    Temperature Comparison
                </button>
            </div>
            {activeTab === 'chat' && <Chat />}
            {activeTab === 'comparison' && <Comparison />}
            {activeTab === 'temperatureComparison' && <TemperatureComparison />}
        </div>
    );
}

export default App;