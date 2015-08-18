angular.module("wust.services").service("FocusService", FocusService);

FocusService.$inject = ["Helpers"];

function FocusService(Helpers) {
    class Tab {
        constructor(index) {
            this.index = index;
        }
        get active() {
            return this._active;
        }
        set active(active) {
            this._active = active;
            // we fire a resize event graph whenever the graph becomes active
            // we use our knowledge to know that index = 1 is the graph
            if (active && this.index === 1)
                setTimeout( () => Helpers.fireWindowResizeEvent(), 200 );
        }
    }

    this.tabViews = _.map([0, 1], i => new Tab(i));

    this.activateTab = function(index) {
        this.tabViews.forEach((t,i) => {
            t.active = i === index;
        });
    };
}
