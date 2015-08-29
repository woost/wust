var xmlHttp = new XMLHttpRequest();
xmlHttp.onreadystatechange = function() {
    if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
        console.log(xmlHttp.responseText);
        var responses = JSON.parse(xmlHttp.responseText);
        if (document.readyState === "interactive" || document.readyState === "complete")
            setErrorMessage();
        else
            document.addEventListener("DOMContentLoaded", setErrorMessage);

        function setErrorMessage() {
            var parentElem = document.createElement("div");
            Object.keys(responses).forEach(function(reporter) {
                var response = responses[reporter];
                if (!response.errors || !response.errors.length)
                    return;

                var errorElem = document.createElement("div");
                errorElem.style.paddingLeft = "100px";
                var errorHead = document.createElement("h2");
                errorHead.appendChild(document.createTextNode(reporter + " reported:"));
                errorElem.appendChild(errorHead);
                response.errors.forEach(function(error) {
                    var elem = document.createElement("div");
                    elem.style.color = "red";
                    elem.appendChild(document.createTextNode(error));
                    errorElem.appendChild(elem);
                });

                var errorSumElem = document.createElement("div");
                errorSumElem.style.paddingTop = "10px";
                errorSumElem.style.fontWeight = "bold";
                errorSumElem.appendChild(document.createTextNode(response.errors.length + " errors"));
                errorElem.appendChild(errorSumElem);
                parentElem.appendChild(errorElem);
            });

            if (parentElem.childElementCount) {
                var targetElem = document.getElementById("content_view") || document.body ||document.createElement("body");
                targetElem.innerHTML = parentElem.outerHTML;
            }
        }
    }
};
xmlHttp.open("GET", "assets/errors.json", true);
xmlHttp.send(null);
