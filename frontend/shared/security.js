(function () {
  const nativeFetch = window.fetch.bind(window);
  let csrfTokenPromise;
  let csrfTokenFetchedAt = 0;
  const csrfTokenRefreshMs = 20 * 60 * 1000;

  function isApiRequest(input) {
    const url = input instanceof Request ? input.url : input;
    const parsedUrl = new URL(url, window.location.href);
    return parsedUrl.origin === window.location.origin && parsedUrl.pathname.startsWith("/api/");
  }

  async function getCsrfToken() {
    if (!csrfTokenPromise || Date.now() - csrfTokenFetchedAt >= csrfTokenRefreshMs) {
      csrfTokenFetchedAt = Date.now();
      csrfTokenPromise = nativeFetch("/api/auth/csrf", {
        credentials: "same-origin",
        cache: "no-store"
      }).then(async (response) => {
        if (!response.ok) throw new Error("Unable to obtain a CSRF token.");
        const data = await response.json();
        if (!data.csrfToken) throw new Error("The server returned no CSRF token.");
        return data.csrfToken;
      }).catch((error) => {
        csrfTokenPromise = null;
        csrfTokenFetchedAt = 0;
        throw error;
      });
    }
    return csrfTokenPromise;
  }

  window.fetch = async function (input, init) {
    const requestInit = Object.assign({}, init || {});
    const requestMethod = (requestInit.method || (input instanceof Request ? input.method : "GET")).toUpperCase();
    const isApi = isApiRequest(input);
    let csrfHeaders;
    if (isApi) requestInit.credentials = requestInit.credentials || "same-origin";

    if (isApi && ["POST", "PUT", "DELETE"].includes(requestMethod)) {
      csrfHeaders = new Headers(requestInit.headers || (input instanceof Request ? input.headers : undefined));
      csrfHeaders.set("X-CSRF-Token", await getCsrfToken());
      requestInit.headers = csrfHeaders;
    }

    let response = await nativeFetch(input, requestInit);
    if (isApi && ["POST", "PUT", "DELETE"].includes(requestMethod)
        && response.status === 403 && !(input instanceof Request)) {
      csrfTokenPromise = null;
      csrfTokenFetchedAt = 0;
      csrfHeaders.set("X-CSRF-Token", await getCsrfToken());
      requestInit.headers = csrfHeaders;
      response = await nativeFetch(input, requestInit);
    }

    const path = window.location.pathname;
    const isAuthPage = path.startsWith("/frontend/auth/");
    if (isApi && response.status === 401 && !isAuthPage) {
      localStorage.removeItem("rentproof_user");
      window.location.replace("/frontend/auth/login.html");
    }
    return response;
  };

  async function requireSession() {
    try {
      const response = await nativeFetch("/api/auth/session", {
        credentials: "same-origin",
        cache: "no-store"
      });
      if (!response.ok) {
        localStorage.removeItem("rentproof_user");
        window.location.replace("/frontend/auth/login.html");
        return false;
      }

      const data = await response.json();
      if (data.landlord) {
        localStorage.setItem("rentproof_user", JSON.stringify(data.landlord));
      }
      return true;
    } catch (error) {
      console.error("Unable to verify the server session.", error);
      localStorage.removeItem("rentproof_user");
      window.location.replace("/frontend/auth/login.html");
      return false;
    }
  }

  async function logout() {
    const response = await window.fetch("/api/auth/logout", { method: "POST" });
    if (!response.ok) throw new Error("The server did not invalidate the session.");
    localStorage.removeItem("rentproof_user");
    csrfTokenPromise = null;
    csrfTokenFetchedAt = 0;
    window.location.replace("/frontend/auth/login.html");
  }

  document.addEventListener("click", async (event) => {
    const target = event.target instanceof Element ? event.target : null;
    const logoutButton = target ? target.closest("#logoutBtn") : null;
    if (!logoutButton) return;

    event.preventDefault();
    event.stopImmediatePropagation();
    if (!window.confirm("Are you sure you want to sign out of RentProof?")) return;

    logoutButton.setAttribute("aria-busy", "true");
    try {
      await logout();
    } catch (error) {
      console.error("Logout failed.", error);
      logoutButton.removeAttribute("aria-busy");
      window.alert("Sign out could not be completed. Please try again.");
    }
  }, true);

  window.RentProofSecurity = { requireSession };
})();
