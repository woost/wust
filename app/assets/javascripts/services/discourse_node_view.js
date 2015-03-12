app.service('DiscourseNodeView', function($state) {
    this.removeFocused = removeFocused;

    function removeFocused(id, removeFunc) {
        return function() {
            removeFunc(id).$promise.then(function(data) {
                toastr.success("Removed node");
                $state.go('home');
            });
        };
    }
});
