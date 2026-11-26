/**
 * DonorNexa — Cascading Province & City Dropdowns
 *
 * Handles dynamic fetching and population of the City select element
 * based on the chosen Province using the asynchronous Fetch API.
 */
document.addEventListener('DOMContentLoaded', function () {
    const provinceSelect = document.getElementById('provinceSelect');
    const citySelect = document.getElementById('citySelect');
    const citySpinner = document.getElementById('citySpinner');
    const cityStatusMessage = document.getElementById('cityStatusMessage');

    if (!provinceSelect || !citySelect) {
        return;
    }

    const preselectedCityId = citySelect.getAttribute('data-preselected');

    /**
     * Updates helper status text under the city dropdown.
     */
    function setStatus(message, isError) {
        if (!cityStatusMessage) return;
        if (!message) {
            cityStatusMessage.textContent = '';
            cityStatusMessage.classList.add('d-none');
            cityStatusMessage.classList.remove('text-danger', 'text-muted');
            return;
        }
        cityStatusMessage.textContent = message;
        cityStatusMessage.classList.remove('d-none');
        if (isError) {
            cityStatusMessage.classList.add('text-danger');
            cityStatusMessage.classList.remove('text-muted');
        } else {
            cityStatusMessage.classList.add('text-muted');
            cityStatusMessage.classList.remove('text-danger');
        }
    }

    /**
     * Toggles visibility of the inline loading spinner.
     */
    function showSpinner(show) {
        if (citySpinner) {
            citySpinner.classList.toggle('d-none', !show);
        }
    }

    /**
     * Fetches cities for the specified province ID and populates the city dropdown.
     *
     * @param {string|number} provinceId
     * @param {string|number|null} cityIdToRestore
     */
    function fetchAndPopulateCities(provinceId, cityIdToRestore) {
        showSpinner(true);
        citySelect.disabled = true;
        citySelect.innerHTML = '<option value="">Loading cities...</option>';
        setStatus('Fetching cities for selected province...', false);

        fetch('/api/locations/provinces/' + encodeURIComponent(provinceId) + '/cities')
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Server returned HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (cities) {
                showSpinner(false);
                citySelect.innerHTML = '';

                if (!cities || cities.length === 0) {
                    const opt = document.createElement('option');
                    opt.value = '';
                    opt.textContent = 'No cities available';
                    citySelect.appendChild(opt);
                    citySelect.disabled = true;
                    setStatus('No cities currently available for this administrative area.', false);
                    return;
                }

                const defaultOpt = document.createElement('option');
                defaultOpt.value = '';
                defaultOpt.textContent = 'Select City';
                citySelect.appendChild(defaultOpt);

                let matchedPreselection = false;
                cities.forEach(function (city) {
                    const opt = document.createElement('option');
                    opt.value = city.id;
                    opt.textContent = city.name;
                    if (cityIdToRestore && String(cityIdToRestore) === String(city.id)) {
                        opt.selected = true;
                        matchedPreselection = true;
                    }
                    citySelect.appendChild(opt);
                });

                citySelect.disabled = false;
                setStatus('');

                if (cityIdToRestore && !matchedPreselection) {
                    citySelect.value = '';
                }
            })
            .catch(function (error) {
                showSpinner(false);
                citySelect.innerHTML = '<option value="">Error loading cities</option>';
                citySelect.disabled = true;
                setStatus('Failed to load cities. Please check your connection and try again.', true);
                console.error('DonorNexa location error:', error);
            });
    }

    // -------------------------------------------------------------------------
    // Initial page load handling
    // -------------------------------------------------------------------------
    if (!provinceSelect.value) {
        citySelect.disabled = true;
        citySelect.innerHTML = '<option value="">Select Province First</option>';
        setStatus('');
    } else {
        // If province is already selected (e.g. form validation error re-render)
        if (citySelect.options.length <= 1) {
            fetchAndPopulateCities(provinceSelect.value, preselectedCityId);
        } else {
            // Options were supplied server-side; keep enabled
            citySelect.disabled = false;
        }
    }

    // -------------------------------------------------------------------------
    // Province change event listener
    // -------------------------------------------------------------------------
    provinceSelect.addEventListener('change', function () {
        const provinceId = this.value;
        if (!provinceId) {
            citySelect.innerHTML = '<option value="">Select Province First</option>';
            citySelect.disabled = true;
            showSpinner(false);
            setStatus('');
            return;
        }
        // Changing province clears previous city selection
        fetchAndPopulateCities(provinceId, null);
    });
});
