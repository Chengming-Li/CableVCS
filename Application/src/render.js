/*
TO DO:
    Create branch buttons and branch selection system(drop down on top left corner)
    Link up frontend with backend
*/

// const buttonOne = document.getElementById('branchButton');
//#region for getting all the elements
const task = document.getElementById("tasks");
const stage = document.getElementById("stagingArea");
const log = document.getElementById("log");
const logText = document.getElementById("logInfo");
const stagedFiles = document.getElementById('staged-files-list');
const unstagedFiles = document.getElementById('unstaged-files-list');
const stagedParent = document.getElementById('staged-holder');
const unstagedParent = document.getElementById('unstaged-holder');
const taskList = document.getElementById('tasksList');
const taskParent = document.getElementById('tasksListHolder');
const getDir = document.getElementById('getDir');
const repoName = document.getElementById('repoName');
const branchDropDown = document.getElementById('branchDropDown');
const newTask = document.getElementById('enter-task');
const newTaskButton = document.getElementById('add-new-task');
const newCommit = document.getElementById('enter-commit');
const newCommitButton = document.getElementById('add-new-commit');
//#endregion

// global variables
const completed = [];
const added = [];
const taskElementList = []
const stagedFilesList = [];
const unstagedFilesList = [];
const commitLog = [];
var currentRepo = "";
var currentBranch = "";
const branches = [];
var topPosition = logText.offsetTop - 17;
const maxTop = topPosition;

function remove(list, element) {
    let index = list.indexOf(element);
    if (index > -1) {
        list.splice(index, 1);
    }
}

//#region for setting up header
getDir.addEventListener('click', () => {
    window.electronAPI.selectFolder().then(result=>{
        if (result[0] !== undefined && currentRepo !== result[0] && result[0].length > 0) {
            resetEverything();
            currentRepo = result[0];
            if (currentRepo.length <= 20) {
                repoName.textContent = currentRepo
            } else {
                repoName.textContent = currentRepo.substring(0, 17) + "...";
            }
            getDir.title = "Directory: " + result[0];
            window.electronAPI.changeDir(result[1])
            console.log("HI")
        }
    })
});

branchDropDown.addEventListener('change', function() {
    const selectedValue = this.value;
    
    if (selectedValue === 'Create New Branch') {
        console.log('NEW BRANCH');
        branchDropDown.value = currentBranch;
    } else {
        currentBranch = selectedValue;
    }
});

// code to insert all the branches to branches list
branches.push("Main")
branches.push("AAA")

branches.forEach(function(item) {
    const option = document.createElement('option');
    option.value = item;
    option.textContent = item; 
    branchDropDown.appendChild(option);
});
const newBranchButton = document.createElement('option');
newBranchButton.value = "Create New Branch";
newBranchButton.textContent = "Create New Branch"; 
newBranchButton.style.backgroundColor = "white";
newBranchButton.style.color = "#38343c";
branchDropDown.appendChild(newBranchButton);
//#endregion

//#region scrolling
/**
 * allows for the scroll wheel to work on the log area
 */
function addScroll(scrollable, parent, offset = 0) {
    let tp = -offset;
    let tpOffset = scrollable.offsetTop
    function scrollCode(event) {
        event.preventDefault();
        const scrollSpeed = .25; // adjust the scrolling speed
        const deltaY = event.deltaY;
        tp -= deltaY * scrollSpeed
        tp = Math.max(Math.min(-(scrollable.clientHeight - parent.clientHeight), 0), Math.min(tp, 0))
        scrollable.style.top = tp + "px";
    }
    function correctTop() {
        scrollable.style.top = Math.max(Math.min(-(scrollable.clientHeight - parent.clientHeight), 0), 
        Math.min(scrollable.offsetTop - tpOffset, 0)) + "px";
    }
    return {
        sc: scrollCode,
        ct: correctTop
      }
}
const logScrollFunc = addScroll(logText, log, 30);
logText.addEventListener('wheel', logScrollFunc.sc);
const taskScrollFunc = addScroll(taskList, taskParent, 0);
taskList.addEventListener('wheel', taskScrollFunc.sc);
const stagedScrollFunc = addScroll(stagedFiles, stagedParent, 0);
stagedFiles.addEventListener('wheel', stagedScrollFunc.sc);
const unstagedScrollFunc = addScroll(unstagedFiles, unstagedParent, -5);
unstagedFiles.addEventListener('wheel', unstagedScrollFunc.sc);
//#endregion

//#region for resizing
/**
 * calculates and returns the width of the log area, based on the widths of the staging and tasks divs
 */
function setThirdWidth() {
    const tasksWidth = task.clientWidth;
    const stageWidth = stage.clientWidth;
    log.style.width = `calc(100vw - ${tasksWidth + stageWidth}px)`
    log.style.left = tasksWidth + "px"
    logScrollFunc.ct()
}
const observer = new ResizeObserver(entries => {
    setThirdWidth();
});
observer.observe(task);
observer.observe(stage);
setThirdWidth();
//#endregion

//#region for creating and deleting elements
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
    let thisList;
    let otherList;
    switch (status) {
        case 0:
            parent = stagedFiles
            newParent = unstagedFiles
            thisList = stagedFilesList
            otherList = unstagedFilesList
            break;
        case 1:
            parent = unstagedFiles
            newParent = stagedFiles
            otherList = stagedFilesList
            thisList = unstagedFilesList
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
        if (parent === stagedFiles) {
            stagedScrollFunc.ct()
        } else {
            unstagedScrollFunc.ct()
        }
        let tmp = parent;
        parent = newParent;
        newParent = tmp;
        remove(thisList, item)
        otherList.push(item);
        tmp = thisList;
        thisList = otherList;
        otherList = tmp;
    });
    thisList.push(item);
  
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
            item.style.backgroundColor = "#495778";
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
    taskElementList.push(item);
}
function resetEverything() {
    stagedFilesList.forEach(function(item) {
        item.remove()
    });
    unstagedFilesList.forEach(function(item) {
        item.remove()
    });
    taskElementList.forEach(function(item) {
        item.remove()
    })
    stagedFilesList.length = 0;
    unstagedFilesList.length = 0;
    taskElementList.length = 0;
    completed.length = 0;
    added.length = 0;
    resetCommitLog()
    branches.length = 0;
    branchDropDown.innerHTML = '';
    currentBranch = ""
}
function addCommit(text, hash) {
    var item = document.createElement("div");
    commitLog.push(item);
    item.className = "commitItem";
    item.appendChild(document.createElement("hr"));
    var hashText = document.createElement('p');
    hashText.innerHTML = hash;
    hashText.setAttribute("id", "hash");
    item.appendChild(hashText);
    var textbox = document.createElement('p');
    textbox.innerHTML = text.replace(/\n/g, "<br>");
    item.appendChild(textbox);
    var button = document.createElement('button');
    button.innerText = "Revert"
    item.appendChild(button);
    button.addEventListener("click", function() {
        console.log(hash);
    });
    logText.appendChild(item);
}
function resetCommitLog() {
    commitLog.forEach(function(item) {
        item.remove()
    })
    commitLog.length = 0;
}
//#endregion

//#region for setting up buttons
newTaskButton.addEventListener('click', function() {
    console.log(newTask.value.replaceAll("\\0", "A"));
});
newCommitButton.addEventListener('click', function() {
    console.log(newCommit.value.replaceAll("\\0", "A"));
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

window.electronAPI.updateUnstaged((event, value) => {
    console.log("Unstaged: " + window.electronAPI.decodeConcatenation(value));
})

window.electronAPI.updateLog((event, value) => {
    console.log("Log: \n[" + window.electronAPI.decodeConcatenation(value) + "]");
})
//#endregion


addFile("One", 0)
addFile("Twoaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0)
addFile("Three", 1)
addFile("Four", 1)
added.push("Four")
addTask("Five")
for (var i = 1; i <= 20; i++) {
    addCommit("Date: 05/16/2023 18:04:31\nAuthor: User\n\nmessage", "c73094bd7dcbcc9adab20647963e8aa531ee7df5")
}
addCommit("Date: 05/16/2023 18:04:31\nAuthor: User\n\nLorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Id donec ultrices tincidunt arcu non sodales neque sodales. Quam lacus suspendisse faucibus interdum posuere. Nunc lobortis mattis aliquam faucibus purus in massa tempor. Odio eu feugiat pretium nibh ipsum consequat nisl vel pretium. Cursus euismod quis viverra nibh cras pulvinar. Amet dictum sit amet justo. Praesent elementum facilisis leo vel fringilla est ullamcorper eget. Est ante in nibh mauris cursus mattis molestie a iaculis. Sociis natoque penatibus et magnis dis parturient montes nascetur. Tincidunt id aliquet risus feugiat in ante. Eu ultrices vitae auctor eu augue ut lectus arcu bibendum. Ante in nibh mauris cursus mattis. Turpis cursus in hac habitasse platea.", "c73094bd7dcbcc9adab20647963e8aa531ee7df5")
/*resetEverything()*/