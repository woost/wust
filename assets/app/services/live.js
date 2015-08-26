angular.module("wust.services").provider("Live", Live);

Live.$inject = [];

function Live() {
    let baseUrl = "";
    this.setBaseUrl = url => baseUrl = url;

    this.$get = get;

    get.$inject = [];
    function get() {
        function subscribe(url, handler) {
            console.log("subscribing to " + url);

            return unsubscribe;

            function unsubscribe() {
                console.log("unsubscribing " + url);
            }

            function onMessage(response) {
                try {
                    var json = JSON.parse(response.responseBody);
                    console.log("incoming message", json);
                    handler(json);
                } catch(e) {
                    console.warn(`illegal message received from atmosphere, will unsubscribe '${url}' now`, response);
                    unsubscribe();
                }
            }
        }

        return {
            subscribe
        };
    }
}
