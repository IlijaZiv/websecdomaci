if (!getToken()) {
    window.location.href = "login.html";
}

async function getMovie() {

    const id = movieId.value;

    const response = await fetch(`${API_URL}/movie/${id}`, {
        headers: authHeader()
    });

    if (handle401(response)) return;

    if (!response.ok) {
        alert("Movie not found");
        return;
    }

    const movie = await response.json();

    movieCard.style.display = "block";

    movieCard.innerHTML = `
        <h3>${movie.title}</h3>
        <p><b>Director:</b> ${movie.director}</p>
        <p><b>Year:</b> ${movie.year}</p>
        <p><b>Running time:</b> ${movie.runningTime} min</p>
        <p>${movie.description}</p>
    `;
}