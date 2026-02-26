window.onload = function() {
  //<editor-fold desc="Changeable Configuration Block">

  const getCookie = (name) => {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(';').shift();
    }
    return null;
  };

  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  window.ui = SwaggerUIBundle({
    url: "/swagger/openapi3.yaml",
    dom_id: '#swagger-ui',
    deepLinking: true,
    requestInterceptor: (req) => {
      const token = getCookie("XSRF-TOKEN");
      if (token) {
        req.headers["X-XSRF-TOKEN"] = token;
      }
      return req;
    },
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });

  //</editor-fold>
};
