(function () {
    if (window.isTeleModInit) return;
    // document-start: The script will be injected as fast as possible
    TeleMod.onDocumentStart();

    // document-body: Check when the body element exists
    let bodyCheckInterval = setInterval(function () {
        if (document.body) {
            TeleMod.onDocumentBody();
            clearInterval(bodyCheckInterval);
        }
    }, 10); // Polling interval to check for body element

    // document-end: When the DOM is fully loaded and parsed
    document.addEventListener('DOMContentLoaded', function () {
        TeleMod.onDocumentEnd();
    });

    // document-idle: After all DOMContentLoaded events and deferred scripts are executed
    window.onload = function () {
        TeleMod.onDocumentIdle();
    };

    // context-menu: Add an event listener for context menu click
    document.addEventListener('contextmenu', function (event) {
        TeleMod.onContextMenu();
    });

    window.isTeleModInit = true;

    window.safeLog = (...args) => {
        if (window.console && window.eruda) {
            console.log(
                "%c[TeleMod]%c >",
                "color: #fff; background: #4CAF50; padding: 2px;",
                "",
                ...args
            );
        } else {
            requestAnimationFrame(() => safeLog(...args));
        }
    }

    window.forceCloseApp = () => {
        const eventData = { return_back: true };
        TelegramWebviewProxy.postEvent("web_app_close", JSON.stringify(eventData));
    }
})();