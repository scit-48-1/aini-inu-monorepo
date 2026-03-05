import { mockApi } from "@/lib/mockApi";

export const analyzeDogImage = async (base64Image: string) => {
  try {
    console.log("Client: Requesting Spring Boot backend analysis...");
    
    const response = await fetch("http://localhost:8080/api/v1/pets/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ image: base64Image }),
    });

    if (!response.ok) throw new Error("Backend not ready");
    return await response.json();

  } catch (error) {
    console.warn("Backend unavailable, using Mock Server for testing.");
    // 🐾 백엔드가 없으면 자동으로 가상 AI 분석 결과를 반환합니다.
    return await mockApi.analyzeImage(base64Image);
  }
};

export const searchMissingDogs = async (description: string) => {
  return "준비 중인 기능입니다.";
};