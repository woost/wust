angular.module("wust.services").service("LeftSideService", LeftSideService);

LeftSideService.$inject = ["store"];

function LeftSideService(store) {
    let leftStore = store.getNamespacedStore("leftSide");

    let visible = leftStore.get("visible") || false;
    Object.defineProperty(this, "visible", {
        get: function() {
            return visible;
        },
        set: function(val) {
            visible = !!val;
            leftStore.set("visible", visible);
        }
    });
}
