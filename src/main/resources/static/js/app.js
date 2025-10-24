class PdfConverterApp {
    constructor() {
        this.websocket = null;
        this.isProcessing = false;
        this.initializeEventListeners();
    }

    initializeEventListeners() {
        document.getElementById('pdfForm').addEventListener('submit', (e) => this.handleFormSubmit(e));
        document.getElementById('browsePdfBtn').addEventListener('click', () => this.browseFolder('pdfFolder'));
        document.getElementById('browseExcelBtn').addEventListener('click', () => this.browseFolder('excelFolder'));
    }

    handleFormSubmit(e) {
        e.preventDefault();
        
        if (this.isProcessing) {
            this.showAlert('Processing is already in progress. Please wait...', 'warning');
            return;
        }

        const pdfFolder = document.getElementById('pdfFolder').value.trim();
        const excelFolder = document.getElementById('excelFolder').value.trim();

        if (!pdfFolder || !excelFolder) {
            this.showAlert('Please provide both PDF and Excel folder paths.', 'danger');
            return;
        }

        // Validate paths
        if (pdfFolder === excelFolder) {
            this.showAlert('PDF and Excel folders cannot be the same. Please choose different folders.', 'warning');
            return;
        }

        this.startProcessing(pdfFolder, excelFolder);
    }

    browseFolder(inputId) {
        const isPdfFolder = inputId === 'pdfFolder';
        const folderType = isPdfFolder ? 'PDF' : 'Excel';
        
        let helpMessage = `📁 ${folderType} Folder Path Help:\n\n`;
        helpMessage += `✅ Enter the complete path to your ${folderType.toLowerCase()} folder\n`;
        helpMessage += `✅ Use forward slashes (/) for all operating systems\n`;
        helpMessage += `✅ Make sure the folder exists and you have read/write permissions\n\n`;
        helpMessage += `💡 Quick Examples:\n`;
        helpMessage += `• Mac/Linux: /Users/username/Desktop/pdfs\n`;
        helpMessage += `• Windows: C:/Users/username/Documents/pdfs\n`;
        helpMessage += `• Relative: ./pdfs (if in same directory as application)\n\n`;
        helpMessage += `💡 Tip: You can also click the suggestion buttons above to auto-fill common paths!`;
        
        this.showAlert(helpMessage, 'info');
        
        // Focus on the input field to help user
        document.getElementById(inputId).focus();
    }

    async startProcessing(pdfFolder, excelFolder) {
        this.isProcessing = true;
        this.showProgressCard();
        this.clearLogs();
        this.resetProgress();
        
        // Connect to WebSocket for real-time updates
        this.connectWebSocket();

        try {
            const response = await fetch('/api/process', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    pdfFolder: pdfFolder,
                    excelFolder: excelFolder
                })
            });

            const result = await response.json();
            
            if (result.success) {
                this.showResults(result.results);
                this.addLogEntry('Processing completed successfully!', 'success');
            } else {
                this.addLogEntry(`Error: ${result.message}`, 'error');
                this.showAlert(result.message, 'danger');
            }
        } catch (error) {
            this.addLogEntry(`Network error: ${error.message}`, 'error');
            this.showAlert(`Network error: ${error.message}`, 'danger');
        } finally {
            this.isProcessing = false;
            this.disconnectWebSocket();
        }
    }

    connectWebSocket() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/progress`;
        
        this.websocket = new WebSocket(wsUrl);
        
        this.websocket.onopen = () => {
            this.addLogEntry('Connected to progress monitoring', 'success');
        };
        
        this.websocket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                this.updateProgress(data.progress, data.message);
            } catch (e) {
                // Handle plain text messages
                this.addLogEntry(event.data, 'info');
            }
        };
        
        this.websocket.onclose = () => {
            this.addLogEntry('Progress monitoring disconnected', 'warning');
        };
        
        this.websocket.onerror = (error) => {
            this.addLogEntry(`WebSocket error: ${error}`, 'error');
        };
    }

    disconnectWebSocket() {
        if (this.websocket) {
            this.websocket.close();
            this.websocket = null;
        }
    }

    updateProgress(progress, message) {
        const progressBar = document.getElementById('progressBar');
        const progressText = document.getElementById('progressText');
        const statusMessage = document.getElementById('statusMessage');
        
        progressBar.style.width = `${progress}%`;
        progressText.textContent = `${progress}%`;
        statusMessage.innerHTML = `<i class="fas fa-info-circle"></i> ${message}`;
        
        this.addLogEntry(message, progress === 100 ? 'success' : 'info');
    }

    showProgressCard() {
        document.getElementById('progressCard').style.display = 'block';
        document.getElementById('resultsCard').style.display = 'none';
    }

    showResults(results) {
        document.getElementById('resultsCard').style.display = 'block';
        const resultsList = document.getElementById('resultsList');
        
        if (results && results.length > 0) {
            results.forEach(result => {
                const resultItem = document.createElement('div');
                resultItem.className = 'results-item';
                resultItem.textContent = result;
                resultsList.appendChild(resultItem);
            });
        } else {
            resultsList.innerHTML = '<div class="alert alert-info">No results to display</div>';
        }
    }

    addLogEntry(message, type = 'info') {
        const logContainer = document.getElementById('logContainer');
        const logEntry = document.createElement('div');
        logEntry.className = `log-entry ${type}`;
        logEntry.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        
        logContainer.appendChild(logEntry);
        logContainer.scrollTop = logContainer.scrollHeight;
    }

    clearLogs() {
        document.getElementById('logContainer').innerHTML = '';
    }

    resetProgress() {
        const progressBar = document.getElementById('progressBar');
        const progressText = document.getElementById('progressText');
        const statusMessage = document.getElementById('statusMessage');
        
        progressBar.style.width = '0%';
        progressText.textContent = '0%';
        statusMessage.innerHTML = '<i class="fas fa-info-circle"></i> Ready to start processing...';
    }

    showAlert(message, type) {
        // Create a temporary alert
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        alertDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 400px; max-width: 600px; white-space: pre-line;';
        alertDiv.innerHTML = `
            <div style="font-family: monospace; font-size: 0.9em;">
                ${message}
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(alertDiv);
        
        // Auto-remove after 8 seconds for help messages
        const timeout = type === 'info' ? 8000 : 5000;
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.parentNode.removeChild(alertDiv);
            }
        }, timeout);
    }
}

// Initialize the application when the page loads
document.addEventListener('DOMContentLoaded', () => {
    const app = new PdfConverterApp();
    
    // Show welcome message
    setTimeout(() => {
        app.showAlert('🎉 Welcome to PDF to Excel Converter!\n\n📝 Enter the paths to your PDF and Excel folders above, then click "Start Processing" to begin conversion.\n\n💡 Use the suggestion buttons for quick path entry!', 'info');
    }, 1000);
});
