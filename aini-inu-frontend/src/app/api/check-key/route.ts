import { GoogleGenAI } from "@google/genai";
import { NextResponse } from "next/server";

export async function GET() {
  const apiKey = process.env.NEXT_PUBLIC_GEMINI_API_KEY;
  if (!apiKey) return NextResponse.json({ error: "No API Key found" });

  const genAI = new GoogleGenAI({ apiKey });

  try {
    // 1. 단순 텍스트 생성 테스트 (가장 기본 모델로 시도)
    // 2.0-flash-exp 대신 더 넓게 지원되는 gemini-1.5-flash-8b 등으로 테스트
    const modelName = "gemini-1.5-flash"; 
    console.log(`Checking model: ${modelName}`);
    
    const response = await genAI.models.generateContent({
      model: modelName,
      contents: [{ role: 'user', parts: [{ text: 'Hello' }] }]
    });

    return NextResponse.json({ 
      status: "Success", 
      modelUsed: modelName,
      responseText: response.text
    });
  } catch (error: any) {
    console.error("Diagnostic Error:", error.message);
    
    // 에러 발생 시, 구글 API로부터 받은 원본 메시지를 분석합니다.
    return NextResponse.json({ 
      status: "Error", 
      error: error.message,
      tip: "모델명을 'gemini-2.0-flash' 또는 'gemini-1.5-flash-8b'로 바꿔보아야 할 것 같습니다."
    }, { status: 500 });
  }
}