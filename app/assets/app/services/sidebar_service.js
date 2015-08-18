angular.module("wust.services").service("SidebarService", SidebarService);

SidebarService.$inject = ["store"];

function SidebarService(store) {
    let sideStore = store.getNamespacedStore("sidebar");

    this.left = new Sidebar("left");
    this.right = new Sidebar("right");

    function Sidebar(name) {
        let visible = sideStore.get(name) || false;
        Object.defineProperty(this, "visible", {
            get: function() {
                return visible;
            },
            set: function(val) {
                visible = !!val;
                sideStore.set(name, visible);
            }
        });
    }
}
