if (getToken()) {
    window.location.href = "app.html";
}

let requestInFlight = false;

async function login() {
    if (requestInFlight) return;

    const btn = document.getElementById("loginBtn");
    const errorEl = document.getElementById("error");

    requestInFlight = true;
    btn.disabled = true;
    errorEl.innerText = "";

    let response;
    try {
        response = await fetch(`${API_URL}/auth/login`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                email: email.value,
                password: password.value
            })
        });
    } catch (networkErr) {
        errorEl.innerText = "Greska: server nije dostupan (port 8080).";
        requestInFlight = false;
        btn.disabled = false;
        return;
    }

    if (response.status === 429) {
        const data = await response.json().catch(() => ({}));
        const retryAfter = parseInt(
            response.headers.get("Retry-After") || data.retryAfterSeconds || "5"
        );
        startCountdown(retryAfter, btn, errorEl);
        requestInFlight = false;
        return;
    }

    requestInFlight = false;
    btn.disabled = false;

    if (!response.ok) {
        errorEl.innerText = "Pogresan e-mail ili lozinka.";
        return;
    }

    const data = await response.json();
    setToken(data.accessToken);
    window.location.href = "app.html";
}

function startCountdown(seconds, btn, errorEl) {
    let remaining = seconds;
    btn.disabled = true;
    errorEl.innerText = "Previse pokusaja! Sacekajte " + remaining + "s.";

    const interval = setInterval(() => {
        remaining--;
        if (remaining <= 0) {
            clearInterval(interval);
            errorEl.innerText = "";
            btn.disabled = false;
        } else {
            errorEl.innerText = "Previse pokusaja! Sacekajte " + remaining + "s.";
        }
    }, 1000);
}
