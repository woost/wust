angular.module("wust.services").service("SidebarService", SidebarService);

SidebarService.$inject = ["store", "Helpers"];

function SidebarService(store, Helpers) {
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
                setTimeout( () => Helpers.fireWindowResizeEvent(), 150 );
            }
        });
    }
}