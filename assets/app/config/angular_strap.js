angular.module("wust.config").config(strapConfig);

strapConfig.$inject = ["$modalProvider", "$tooltipProvider", "$dropdownProvider"];

function strapConfig($modalProvider, $tooltipProvider, $dropdownProvider) {
  angular.extend($modalProvider.defaults, {
    html: true,
    backdrop: "static",
    keyboard: true
  });
  angular.extend($tooltipProvider.defaults, {
    animation: "",
    container: "body",
  });
  angular.extend($dropdownProvider.defaults, {
    animation: "",
    container: "body",
  });
}
