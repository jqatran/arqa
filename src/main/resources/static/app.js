"use strict";

const SUPPORTED_LANGUAGES = ["kk", "ru", "en"];
const DEFAULT_LANGUAGE = "ru";
const LANGUAGE_STORAGE_KEY = "arqa.language";
const TOAST_DURATION_MILLISECONDS = 3200;

const languageButtons = document.querySelectorAll("[data-language]");
const selectedDateInput = document.getElementById("selected-date");
const statusMessage = document.getElementById("status-message");
const tripsBody = document.getElementById("trips-body");
const emptyDayMessage = document.getElementById("empty-day-message");
const addTripDialog = document.getElementById("add-trip-dialog");
const addTripForm = document.getElementById("add-trip-form");
const formMessage = document.getElementById("form-message");
const toast = document.getElementById("toast");
const commissionPercentInput = document.getElementById("form-commission-percent");
const commissionInput = document.getElementById("form-commission");
const commissionHint = document.getElementById("commission-hint");

let currentLanguage = detectInitialLanguage();
let toastTimer = null;

// One identifier per filled-in form: pressing "Add" twice sends the same trip, which the server de-duplicates.
let pendingTripIdentifier = crypto.randomUUID();

function texts() {
    return translations[currentLanguage];
}

function detectInitialLanguage() {
    try {
        const savedLanguage = localStorage.getItem(LANGUAGE_STORAGE_KEY);
        if (SUPPORTED_LANGUAGES.includes(savedLanguage)) {
            return savedLanguage;
        }
    } catch (error) {
        // storage may be blocked; fall through to the browser language
    }
    const browserLanguage = (navigator.language || "").slice(0, 2).toLowerCase();
    return SUPPORTED_LANGUAGES.includes(browserLanguage) ? browserLanguage : DEFAULT_LANGUAGE;
}

function applyLanguageToPage() {
    document.documentElement.lang = currentLanguage;
    document.title = texts().title;
    for (const button of languageButtons) {
        button.setAttribute("aria-pressed", String(button.dataset.language === currentLanguage));
    }
    for (const element of document.querySelectorAll("[data-i18n]")) {
        element.textContent = texts()[element.dataset.i18n];
    }
    for (const element of document.querySelectorAll("[data-i18n-aria-label]")) {
        element.setAttribute("aria-label", texts()[element.dataset.i18nAriaLabel]);
    }
}

function formatMoney(amount) {
    return new Intl.NumberFormat(texts().formatLocale).format(amount) + " ₸";
}

function setText(elementIdentifier, text) {
    document.getElementById(elementIdentifier).textContent = text;
}

function showMessage(element, text, kind) {
    element.hidden = !text;
    element.textContent = text || "";
    element.className = kind || "";
}

function showToast(text) {
    toast.textContent = text;
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        toast.hidden = true;
    }, TOAST_DURATION_MILLISECONDS);
}

function shiftDate(isoDate, dayOffset) {
    const [year, month, day] = isoDate.split("-").map(Number);
    const shiftedDate = new Date(Date.UTC(year, month - 1, day + dayOffset));
    return shiftedDate.toISOString().slice(0, 10);
}

function todayIsoDate() {
    const now = new Date();
    const localTime = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
    return localTime.toISOString().slice(0, 10);
}

// The server keeps each trip's own UTC offset; reading the clock digits from the text avoids browser time-zone shifts.
function clockTime(isoDateTimeWithOffset) {
    return isoDateTimeWithOffset.slice(11, 16);
}

// Every API call states the language, so server messages (validation, conflicts, bad parameters) arrive translated.
function apiFetch(url, options) {
    const requestOptions = options || {};
    const headers = Object.assign({"Accept-Language": currentLanguage}, requestOptions.headers);
    return fetch(url, Object.assign({}, requestOptions, {headers}));
}

async function fetchJson(url) {
    const response = await apiFetch(url);
    if (!response.ok) {
        throw new Error(texts().serverError(response.status));
    }
    return response.json();
}

function renderPaymentBar(summary) {
    const cashRevenue = summary.cash.revenue;
    const cardRevenue = summary.card.revenue;
    const totalRevenue = cashRevenue + cardRevenue;
    const cashPercent = totalRevenue === 0 ? 0 : (cashRevenue / totalRevenue) * 100;
    document.getElementById("payment-bar-cash").style.width = cashPercent + "%";
    document.getElementById("payment-bar-card").style.width = (totalRevenue === 0 ? 0 : 100 - cashPercent) + "%";
}

function renderSummary(summary) {
    setText("summary-trip-count", String(summary.tripCount));
    setText("summary-revenue", formatMoney(summary.revenue));
    setText("summary-commission", formatMoney(summary.commission));
    setText("summary-net-earnings", formatMoney(summary.netEarnings));
    setText("hero-caption", texts().revenue + " − " + texts().commission.toLowerCase());
    setText("summary-cash", formatMoney(summary.cash.revenue));
    setText("summary-cash-note", texts().paymentNote(summary.cash.tripCount, formatMoney(summary.cash.commission)));
    setText("summary-card", formatMoney(summary.card.revenue));
    setText("summary-card-note", texts().paymentNote(summary.card.tripCount, formatMoney(summary.card.commission)));
    renderPaymentBar(summary);
}

function createCell(text, className, label) {
    const cell = document.createElement("td");
    cell.textContent = text;
    if (className) {
        cell.className = className;
    }
    cell.dataset.label = label; // shown as the caption when the table turns into cards on phones
    return cell;
}

function createPaymentCell(payment) {
    const cell = createCell("", "", texts().columnPayment);
    const badge = document.createElement("span");
    badge.className = "badge badge-" + (payment === "cash" ? "cash" : "card");
    badge.textContent = texts().paymentLabels[payment] || payment;
    cell.append(badge);
    return cell;
}

function renderTrips(trips) {
    tripsBody.replaceChildren();
    for (const trip of trips) {
        const row = document.createElement("tr");
        row.append(
            createCell(clockTime(trip.start) + " – " + clockTime(trip.end), "time", texts().columnTime),
            createCell(formatMoney(trip.amount), "number", texts().columnAmount),
            createPaymentCell(trip.payment),
            createCell(formatMoney(trip.commission), "number", texts().commission),
            createCell(formatMoney(trip.amount - trip.commission), "number", texts().netEarnings));
        tripsBody.append(row);
    }
    emptyDayMessage.hidden = trips.length > 0;
}

async function showDay(isoDate) {
    selectedDateInput.value = isoDate;
    showMessage(statusMessage, texts().loading);
    try {
        const encodedDate = encodeURIComponent(isoDate);
        const [summary, trips] = await Promise.all([
            fetchJson("/api/summary?date=" + encodedDate),
            fetchJson("/api/trips?date=" + encodedDate)]);
        if (selectedDateInput.value !== isoDate) {
            return; // the user already switched to another day
        }
        renderSummary(summary);
        renderTrips(trips);
        showMessage(statusMessage, "");
    } catch (error) {
        showMessage(statusMessage, texts().loadError + error.message, "error");
    }
}

async function latestDayWithTrips() {
    const datesWithTrips = await fetchJson("/api/trip-dates");
    return datesWithTrips.length > 0 ? datesWithTrips[datesWithTrips.length - 1] : todayIsoDate();
}

function localOffsetText(dateTimeLocalValue) {
    const offsetMinutes = -new Date(dateTimeLocalValue).getTimezoneOffset();
    const sign = offsetMinutes >= 0 ? "+" : "-";
    const absoluteMinutes = Math.abs(offsetMinutes);
    const hours = String(Math.floor(absoluteMinutes / 60)).padStart(2, "0");
    const minutes = String(absoluteMinutes % 60).padStart(2, "0");
    return sign + hours + ":" + minutes;
}

function withSeconds(dateTimeLocalValue) {
    return dateTimeLocalValue.length === 16 ? dateTimeLocalValue + ":00" : dateTimeLocalValue;
}

function describeProblem(errorBody) {
    if (errorBody && errorBody.violations && errorBody.violations.length > 0) {
        return errorBody.violations
            .map((violation) => (texts().fieldNames[violation.fieldName] || violation.fieldName) + ": " + violation.message)
            .join("; ");
    }
    return (errorBody && errorBody.message) || texts().unknownError;
}

function showCommissionHint(text, kind) {
    commissionHint.textContent = text;
    commissionHint.className = "commission-hint" + (kind ? " " + kind : "");
}

function resetCommissionHint() {
    showCommissionHint(texts().commissionHelp);
}

// Whole tenge, like every other amount. Percent is taken in hundredths so 15.5 % does not suffer from float noise.
function commissionFromPercent(amount, percent) {
    const percentInHundredths = Math.round(percent * 100);
    return Math.round((amount * percentInHundredths) / 10000);
}

// Fills the manual commission field from the percent; the result stays editable and is what gets sent to the server.
function calculateCommission() {
    const amount = Number(document.getElementById("form-amount").value);
    if (!Number.isFinite(amount) || amount <= 0) {
        showCommissionHint(texts().amountRequiredForCalculation, "error");
        return;
    }
    const percentText = commissionPercentInput.value.trim();
    const percent = Number(percentText);
    if (percentText === "" || !Number.isFinite(percent) || percent < 0 || percent > 100) {
        showCommissionHint(texts().percentInvalid, "error");
        return;
    }
    const commission = commissionFromPercent(amount, percent);
    commissionInput.value = String(commission);
    commissionInput.dispatchEvent(new Event("input", {bubbles: true})); // changed data: the trip needs its own identifier
    showCommissionHint(texts().commissionCalculated(percent, formatMoney(amount), formatMoney(commission)), "success");
}

// The form is hidden until asked for. Opening it starts a new trip: fresh identifier, the shown day pre-filled.
function openAddTripDialog() {
    addTripForm.reset();
    pendingTripIdentifier = crypto.randomUUID();
    showMessage(formMessage, "");
    resetCommissionHint();
    const shownDay = selectedDateInput.value || todayIsoDate();
    document.getElementById("form-start").value = shownDay + "T09:00";
    document.getElementById("form-end").value = shownDay + "T09:30";
    addTripDialog.showModal();
    document.getElementById("form-amount").focus();
}

function closeAddTripDialog() {
    addTripDialog.close();
}

async function submitTrip(event) {
    event.preventDefault();
    const startValue = document.getElementById("form-start").value;
    const endValue = document.getElementById("form-end").value;
    if (!startValue || !endValue) {
        showMessage(formMessage, texts().timesRequired, "error");
        return;
    }
    const tripPayload = {
        id: pendingTripIdentifier,
        start: withSeconds(startValue) + localOffsetText(startValue),
        end: withSeconds(endValue) + localOffsetText(endValue),
        amount: Number(document.getElementById("form-amount").value),
        payment: document.getElementById("form-payment").value,
        commission: Number(document.getElementById("form-commission").value)
    };
    try {
        const response = await apiFetch("/api/trips", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(tripPayload)
        });
        const responseBody = await response.json().catch(() => null);
        if (!response.ok) {
            showMessage(formMessage, describeProblem(responseBody), "error");
            return;
        }
        closeAddTripDialog();
        showToast(response.status === 201 ? texts().tripAdded : texts().tripAlreadyAdded);
        await showDay(responseBody.start.slice(0, 10));
    } catch (error) {
        showMessage(formMessage, texts().sendError + error.message, "error");
    }
}

function changeLanguage(newLanguage) {
    currentLanguage = newLanguage;
    try {
        localStorage.setItem(LANGUAGE_STORAGE_KEY, newLanguage);
    } catch (error) {
        // the choice just will not be remembered
    }
    applyLanguageToPage();
    showMessage(formMessage, "");
    if (selectedDateInput.value) {
        showDay(selectedDateInput.value); // reload: numbers and labels depend on the language
    }
}

for (const button of languageButtons) {
    button.addEventListener("click", () => changeLanguage(button.dataset.language));
}
document.getElementById("previous-day").addEventListener("click", () => showDay(shiftDate(selectedDateInput.value, -1)));
document.getElementById("next-day").addEventListener("click", () => showDay(shiftDate(selectedDateInput.value, 1)));
document.getElementById("latest-day").addEventListener("click", async () => showDay(await latestDayWithTrips()));
selectedDateInput.addEventListener("change", () => {
    if (selectedDateInput.value) {
        showDay(selectedDateInput.value);
    }
});

document.getElementById("open-add-trip").addEventListener("click", openAddTripDialog);
document.getElementById("close-add-trip").addEventListener("click", closeAddTripDialog);
document.getElementById("cancel-add-trip").addEventListener("click", closeAddTripDialog);
addTripDialog.addEventListener("click", (event) => {
    if (event.target === addTripDialog) {
        closeAddTripDialog(); // a click on the dimmed backdrop
    }
});
document.getElementById("calculate-commission").addEventListener("click", calculateCommission);
commissionPercentInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        event.preventDefault(); // Enter in the percent field means "calculate", not "send the whole form"
        calculateCommission();
    }
});
commissionInput.addEventListener("input", (event) => {
    if (event.isTrusted) {
        resetCommissionHint(); // typed by hand: the value no longer comes from the percent
    }
});
addTripForm.addEventListener("submit", submitTrip);
addTripForm.addEventListener("input", () => {
    // Editing the form means a different trip: it must get its own identifier.
    pendingTripIdentifier = crypto.randomUUID();
});

applyLanguageToPage();
latestDayWithTrips().then(showDay).catch((error) =>
    showMessage(statusMessage, texts().loadError + error.message, "error"));
