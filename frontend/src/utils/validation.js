/**
 * Input validation for observer geographic coordinates.
 */

function validateObserver(latitude, longitude) {
  let latError = null;
  let lonError = null;

  if (latitude === "" || latitude === null || latitude === undefined) {
    latError = "Latitude is required";
  } else {
    const latNum = Number(latitude);
    if (isNaN(latNum)) {
      latError = "Must be a valid number";
    } else if (latNum < -90 || latNum > 90) {
      latError = "Latitude must be between -90 and 90";
    }
  }

  if (longitude === "" || longitude === null || longitude === undefined) {
    lonError = "Longitude is required";
  } else {
    const lonNum = Number(longitude);
    if (isNaN(lonNum)) {
      lonError = "Must be a valid number";
    } else if (lonNum < -180 || lonNum > 180) {
      lonError = "Longitude must be between -180 and 180";
    }
  }

  return {
    isValid: !latError && !lonError,
    latError,
    lonError,
  };
}

const validation = {
  validateObserver,
};

export default validation;
