// const buttonOne = document.getElementById('branchButton');

const task = document.getElementById("tasks");
const stage = document.getElementById("stagingArea");
const log = document.getElementById("log");
const logText = document.getElementById("logInfo");

var topPosition = logText.offsetTop - 17;
const maxTop = topPosition;

function setThirdWidth() {
    const tasksWidth = task.clientWidth;
    const stageWidth = stage.clientWidth;
    log.style.width = `calc(100vw - ${tasksWidth + stageWidth}px)`
    log.style.left = tasksWidth + "px"
    logText.style.top = Math.max(Math.min(topPosition, maxTop), -(logText.clientHeight - log.clientHeight + 17)) + "px";
}
const observer = new ResizeObserver(entries => {
    setThirdWidth();
});
observer.observe(task);
observer.observe(stage);
setThirdWidth();

const stagedFiles = document.getElementById('staged-files');

function addItem(name) {
    // Create a new item div
    var item = document.createElement("div");
    item.className = "item";
  
    // Create a new text box
    var textbox = document.createElement("input");
    textbox.type = "text";
    textbox.value = name;
    item.appendChild(textbox);
  
    // Create a new delete button
    var deleteButton = document.createElement("button");
    deleteButton.textContent = "Delete";
    deleteButton.onclick = function() {
        stagedFiles.removeChild(item);
    };
    item.appendChild(deleteButton);
  
    // Add the item to the list
    stagedFiles.appendChild(item);
}
addItem("One")
addItem("Two")

// handles messages received by render.js
window.electronAPI.onGetMessage((event, value) => {
    console.log(value);
})
// handles errors received by render.js
window.electronAPI.onGetError((event, value) => {
    alert("ERROR: " + value);
})

window.electronAPI.updateBranch((event, value) => {
    console.log("Branches: " + window.electronAPI.decodeConcatenation(value));
})

window.electronAPI.updateStaged((event, value) => {
    console.log("Staged: " + window.electronAPI.decodeConcatenation(value));
})

window.electronAPI.updateModified((event, value) => {
    console.log("Modified: " + window.electronAPI.decodeConcatenation(value));
})

window.electronAPI.updateUntracked((event, value) => {
    console.log("Untracked: " + window.electronAPI.decodeConcatenation(value));
})

window.electronAPI.updateRemoved((event, value) => {
    console.log("Removed: " + window.electronAPI.decodeConcatenation(value));
})

// adds a listener to button that'll call the code inside
/*buttonOne.addEventListener('click', () => {
    window.electronAPI.init("C:\\Users\\malic\\Downloads\\Test")
    window.electronAPI.branch("Branch")
    window.electronAPI.branch("Other Branch")
})*/

log.addEventListener('wheel', (event) => {
    const minTop = logText.clientHeight - log.clientHeight + 17;
    event.preventDefault();
  
    const scrollSpeed = .25; // adjust the scrolling speed
    const deltaY = event.deltaY;
    topPosition -= deltaY * scrollSpeed
    topPosition = Math.max(Math.min(topPosition, maxTop), -minTop)
    logText.style.top = topPosition + "px";
});