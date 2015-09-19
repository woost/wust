var xmlHttp = new XMLHttpRequest();
xmlHttp.onreadystatechange = function() {
    if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
        // console.log(xmlHttp.responseText);
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

                var errorElem = document.createElement("pre");
                errorElem.style.padding = "20px";
                errorElem.style.margin = "10px";
                var errorHead = document.createElement("h2");
                errorHead.appendChild(document.createTextNode(reporter + " reported:"));
                errorElem.appendChild(errorHead);
                response.errors.forEach(function(error) {
                    var elem = document.createElement("div");
                    var formattedError = error
                        .replace(/\u001b\[31m/g,"<span style='color:red'>")
                        .replace(/\u001b\[32m/g,"<span style='color:green'>")
                        .replace(/\u001b\[33m/g,"<span style='color:yellow'>")
                        .replace(/\u001b\[34m/g,"<span style='color:blue'>")
                        .replace(/\u001b\[35m/g,"<span style='color:magenta'>")
                        .replace(/\u001b\[36m/g,"<span style='color:cyan'>")
                        .replace(/\u001b\[37m/g,"<span style='color:white'>")
                        .replace(/\u001b\[39m/g,"</span>") // foreground default
                        .replace(/\u001b\[0m/g,"</span>"); // reset all attributes
                    var formattedElem = document.createElement("span");
                    elem.innerHTML = formattedError;
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
                document.body.innerHTML = parentElem.outerHTML;
                document.body.style.overflow = "auto";
            }
        }
    }
};
xmlHttp.open("GET", "assets/errors.json", true);
xmlHttp.send(null);
