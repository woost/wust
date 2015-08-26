angular.module("wust.services").service("SidebarService", SidebarService);

SidebarService.$inject = ["store", "Helpers"];

function SidebarService(store, Helpers) {
    let sideStore = store.getNamespacedStore("sidebar");

    this.left = new Sidebar("left");
    this.right = new Sidebar("right");

    function Sidebar(name) {
        let obj = sideStore.get(name) || {
            visible: false,
            fullscreen: false
        };
        Object.defineProperty(this, "visible", {
            get: function() {
                return obj.visible;
            },
            set: function(val) {
                obj.visible = !!val;
                sideStore.set(name, obj);
                setTimeout( () => Helpers.fireWindowResizeEvent(), 150 );
            }
        });
        Object.defineProperty(this, "fullscreen", {
            get: function() {
                return obj.fullscreen;
            },
            set: function(val) {
                obj.fullscreen = !!val;
                sideStore.set(name, obj);
            }
        });
    }
}
