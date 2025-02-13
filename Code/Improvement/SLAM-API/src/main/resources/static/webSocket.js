const socketURL = 'wss://ws.ifelse.io';
const socket = new WebSocket(socketURL);

// Displays the message in the console
function showMessage(message, received, error, info) {
    const container = document.getElementById("consoleOutput");
    const messageElement = document.createElement("div");
    messageElement.textContent = message;
    messageElement.classList.add("message");

    if (received) {
        if (error) {
            messageElement.classList.add("errorMessage");
        } else if (info) {
            messageElement.classList.add("infoMessage");
        } else {
            messageElement.classList.add("receivedMessage");
        }
    } else {
        messageElement.classList.add("sentMessage");
    }

    container.appendChild(messageElement);
    scrollDownInConsole();
}

// Sends a message to the WebSocket server
function sendMessage(message) {
    console.log(`Sending message: ${message}`);
    socket.send(message);

    showMessage(message, false, false, false);

    document.getElementById("consoleInput").value = "";
}

// Event listener for when the connection is open
socket.addEventListener('open', () => {
    console.log('WebSocket connection established.');
    showMessage("Connection established.", true, false, true);
});

// Event listener for when a message is received
socket.addEventListener('message', (event) => {
    showMessage(event.data, true, false, false);
});

// Event listener for when the connection is closed
socket.addEventListener('close', () => {
    showMessage("Connection closed.", true, true, false);
});

// Event listener for errors
socket.addEventListener('error', (error) => {
    showMessage(`An error occurred: ${error}`, true, true, false);
    console.error('WebSocket error:', error);
});

function scrollDownInConsole() {
    const element = document.getElementById("consoleOutput");
    element.scrollTop = element.scrollHeight;
}