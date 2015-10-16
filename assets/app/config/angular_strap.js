angular.module("wust.config").config(strapConfig);

strapConfig.$inject = ["$modalProvider", "$tooltipProvider"];

function strapConfig($modalProvider, $tooltipProvider) {
  angular.extend($modalProvider.defaults, {
    html: true,
    backdrop: "static",
    keyboard: true
  });
  angular.extend($tooltipProvider.defaults, {
    animation: "",
    container: "body"
  });
}
