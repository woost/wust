angular.module("wust.services").service("FullscreenService", FullscreenService);

FullscreenService.$inject = ["SearchService", "ZenService", "SidebarService"];

function FullscreenService(SearchService, ZenService, SidebarService) {
    this.hideFullscreens = function() {
        SearchService.search.resultsVisible = false;
        SidebarService.left.fullscreen = false;
        ZenService.hide();
    };

    Object.defineProperty(this, "hasFullscreen", {
        get: () => {
            return SearchService.search.resultsVisible || SidebarService.left.fullscreen || ZenService.visible;
        }
    });
}
