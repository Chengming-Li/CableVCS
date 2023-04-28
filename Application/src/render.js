// const buttonOne = document.getElementById('branchButton');
//#region for getting all the elements
const task = document.getElementById("tasks");
const stage = document.getElementById("stagingArea");
const log = document.getElementById("log");
const logText = document.getElementById("logInfo");
const stagedFiles = document.getElementById('staged-files-list');
const unstagedFiles = document.getElementById('unstaged-files-list');
const taskList = document.getElementById('tasksList');
//#endregion

const completed = [];
const added = [];
var topPosition = logText.offsetTop - 17;
const maxTop = topPosition;

function remove(list, element) {
    let index = list.indexOf(element);
    if (index > -1) {
        list.splice(index, 1);
    }
}

//#region for resizing
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
        newParent.appendChild(item);
        let tmp = parent;
        parent = newParent;
        newParent = tmp;
    });
  
    // Add the item to the list
    parent.appendChild(item);
}
function addTask(name) {
    let parent = taskList
    let clicked = false;
    let newlyAdded = (added.includes(name));
    var item = document.createElement("div");
    item.className = "item";
  
    // Create a new text box
    var textbox = document.createElement('p');
    textbox.textContent = name;
    item.appendChild(textbox);
    item.addEventListener("click", function() {
        if (clicked) {
            item.style.backgroundColor = "";
            remove(completed, name)
            if (newlyAdded) {
                added.push(name);
            }
        } else {
            item.style.backgroundColor = "#1e2431";
            if (!newlyAdded) {
                completed.push(name);
            } else {
                remove(added, name);
            }
        }
        clicked = !clicked;
    });
  
    // Add the item to the list
    parent.appendChild(item);
}

//#region scrolling
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
taskList.addEventListener('wheel', (event) => {
    const scrollSpeed = .25; // adjust the scrolling speed
    const deltaY = event.deltaY;
});
//#endregion

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
added.push("Five")
addTask("Five")
addTask("Six")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")
addTask("Seven")
addTask("Eight")