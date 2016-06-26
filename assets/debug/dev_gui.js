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
        devGui.style.opacity = 0.5;
        document.body.appendChild(devGui);

        devGui.appendChild(loginLink("a"));
        devGui.appendChild(loginLink("b"));
        devGui.appendChild(loginLink("c"));
        devGui.appendChild(loginLink("d"));
        devGui.appendChild(loginLink(randomString(5), "random"));
    }

    function randomString(length) {
        return Math.random().toString(36).replace(/[^a-z]+/g, "").substr(0, length);
    }

    function loginLink(name, linkname) {
        var link = linkname || name;
        var randLogin = rawElem("<a href=\"#\" title=\"login as " + name + "\">" + link + "</a> ");
        randLogin.onclick = function() {
            loginAs(name);
        };
        return randLogin;
    }

    function loginAs(name) {
        Auth.logout(false);
        Auth.register({
            identifier: name,
            password: "hans"
        });
        Auth.login({
            identifier: name,
            password: "hans"
        });
    }
});
