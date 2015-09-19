angular.module("wust").run(function(Auth) {
    if (document.readyState === "interactive" || document.readyState === "complete")
        createDevGui();
    else
        document.addEventListener("DOMContentLoaded", createDevGui);

    function rawElem(raw) {
        var elem = document.createElement("span");
        elem.innerHTML = raw;
        return elem;
    }

    function createDevGui() {
        var devGui = document.createElement("div");
        devGui.style.position = "absolute";
        devGui.style.zIndex = 99999;
        devGui.style.top = 0;
        devGui.style.right = 0;
        document.body.appendChild(devGui);

        var randLogin = rawElem("<a href=\"#\">random login</a>");
        randLogin.onclick = randomLogin;
        devGui.appendChild(randLogin);
    }

    function randomLogin() {
        Auth.logout(false);
        Auth.register({
            identifier: Math.random().toString(36).substring(7),
            password: "hans"
        });
    }
});
