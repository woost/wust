angular.module("wust.config").config(HumaneConfig);

HumaneConfig.$inject = [];

function HumaneConfig() {
    humane.error = humane.spawn({
        addnCls: "humane-libnotify-error",
        timeout: 5000,
        clickToClose: true
    });

    humane.success = humane.spawn({
        addnCls: "humane-libnotify-info",
        timeout: 2500,
    });
}
