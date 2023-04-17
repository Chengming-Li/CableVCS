const buttonOne = document.getElementById('buttonID');
window.electronAPI.onGetMessage((event, value) => {
    console.log(value);
})
// adds a listener to title that'll call the code inside
buttonOne.addEventListener('click', () => {
    window.electronAPI.testDialogue("HALLO")
})