angular.module("wust").config(HumaneConfig);

HumaneConfig.$inject = [];

function HumaneConfig() {
    humane.error = humane.spawn({
        addnCls: "humane-libnotify-error",
        timeout: 0,
        clickToClose: true
    });

    humane.success = humane.spawn({
        addnCls: "humane-libnotify-info",
        timeout: 700
    });
}
