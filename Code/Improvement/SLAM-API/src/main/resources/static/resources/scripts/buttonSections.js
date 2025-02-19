function showButtons(mode) {
    const buttonsContainer = document.getElementById("buttonsContainer");
    switch (mode) {
        case "Remote-Control":
            buttonsContainer.innerHTML = `
                <h2>Directions</h2>
                <button id="forwardButton" class="button">Forward</button>
                <button id="backwardButton" class="button">Backward</button>
                <button id="leftButton" class="button">Left</button>
                <button id="rightButton" class="button">Right</button>
                <h2>Turn</h2>
                <button id="turnLeftButton" class="button">left</button>
                <button id="turnRightButton" class="button">right</button>
                <h2>Speed</h2>
                <form id="speedControlForm">
                    <input type="number" min="0" max="100" id="speedControlInput" placeholder="Enter speed here (%)" autocomplete="off">
                    <button id="speedControlSubmit" class="button">Send</button>
                </form>
            `;
            break;
        default:
            buttonsContainer.innerHTML = "";
    }
}

document.getElementById("buttonsSelect").value = "Please select";
document.getElementById("buttonsSelect").addEventListener("change", (event) => {
    const mode = event.target.value;
    sendMessage(`{"mode": "${mode}"}`);
    showButtons(mode);
});

document.addEventListener("keydown", (event) => {
    if (event.key === "w" || event.key === "W") {
        console.log("The 'w' key was pressed.");
        // Add your logic here to handle the "w" key press
    }
});