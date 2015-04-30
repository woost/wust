angular.module("wust").config(function() {
    humane.error = humane.spawn({
        addnCls: "humane-libnotify-error",
        timeout: 0,
        clickToClose: true
    });

    humane.success = humane.spawn({
        timeout: 700
    });
});
