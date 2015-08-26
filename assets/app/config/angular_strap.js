angular.module("wust.config").config(modalConfig);

modalConfig.$inject = ["$modalProvider"];

function modalConfig($modalProvider) {
  angular.extend($modalProvider.defaults, {
    html: true
  });
}
