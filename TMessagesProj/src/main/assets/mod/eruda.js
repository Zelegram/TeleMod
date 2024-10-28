(function () {
    console.log("Starting to load Eruda");

    if (window.eruda) {
        console.log("Eruda is already loaded.");
        return;
    }

    var define;
    if (window.define) {
        define = window.define;
        window.define = null;
    }
    var script = document.createElement('script');
    script.src = '//cdn.jsdelivr.net/npm/eruda';
    script.onload = function () {
        eruda.init();
        if (define) {
            window.define = define;
        }
        hideEntryButton();
    }

    document.body.appendChild(script);

    window.toggleEruda = function () {
        eruda._devTools.toggle();
    }

    function hideEntryButton() {
        const style = document.createElement('style');
        style.textContent = `.eruda-entry-btn { display: none !important; }`;
        const root = eruda._shadowRoot || eruda._container;
        root.appendChild(style);
    }
})();