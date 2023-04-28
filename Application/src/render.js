// const buttonOne = document.getElementById('branchButton');
//#region for getting all the elements
const task = document.getElementById("tasks");
const stage = document.getElementById("stagingArea");
const log = document.getElementById("log");
const logText = document.getElementById("logInfo");
const stagedFiles = document.getElementById('staged-files');
const unstagedFiles = document.getElementById('unstaged-files');
const taskList = document.getElementById('taskList');
//#endregion

const completed = [];
var topPosition = logText.offsetTop - 17;
const maxTop = topPosition;

//#region for setting up scrolling
/**
 * calculates and returns the width of the log area, based on the widths of the staging and tasks divs
 */
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
//#endregion

/**
 * Adds a new item in the lists
 * @param {*} name text shown on the item
 * @param {*} status determines which list the item is added to:
 *      0: staged files
 *      1: unstaged files
 *      2: to do list
 *      3: completed tasks
 */
function addFile(name, status) {
    let parent
    let newParent
    switch (status) {
        case 0:
            parent = stagedFiles
            newParent = unstagedFiles
            break;
        case 1:
            parent = unstagedFiles
            newParent = stagedFiles
            break;
        default:
            parent = taskList;
            newParent = null;
    }
    // Create a new item div
    var item = document.createElement("div");
    item.className = "item";
  
    // Create a new text box
    var textbox = document.createElement('p');
    textbox.textContent = name;
    item.appendChild(textbox);
    item.addEventListener("click", function() {
        parent.removeChild(item);
        if (newParent !== null) {
            newParent.appendChild(item);
            let temp = parent;
            parent = newParent;
            newParent = temp;
        } else {
            completed.push(name);
        }
        
    });
  
    // Add the item to the list
    parent.appendChild(item);
}

/**
 * allows for the scroll wheel to work on the log area
 */
log.addEventListener('wheel', (event) => {
    const minTop = logText.clientHeight - log.clientHeight + 17;
    event.preventDefault();
  
    const scrollSpeed = .25; // adjust the scrolling speed
    const deltaY = event.deltaY;
    topPosition -= deltaY * scrollSpeed
    topPosition = Math.max(Math.min(topPosition, maxTop), -minTop)
    logText.style.top = topPosition + "px";
});

//#region for setting up IPC
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
//#endregion

addFile("One", 0)
addFile("Twoaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0)
addFile("Three", 1)
addFile("Four", 1)
addFile("Five", 2)
addFile("Six", 2)
addFile("Seven", 2)
addFile("Eight", 3)