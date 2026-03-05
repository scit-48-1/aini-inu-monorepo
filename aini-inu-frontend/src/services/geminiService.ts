export const analyzeDogImage = async (base64Image: string) => {
  console.log("Client: Requesting Spring Boot backend analysis...");

  const response = await fetch("http://localhost:8080/api/v1/pets/analyze", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ image: base64Image }),
  });

  if (!response.ok) throw new Error("Backend error: pet analysis failed");
  return await response.json();
};

export const searchMissingDogs = async (description: string) => {
  return "준비 중인 기능입니다.";
};