import { apiClient } from './client';

// --- Types ---

export interface PetCreateRequest {
  name: string;
  breedId: number;
  birthDate: string;
  gender: string;
  size: string;
  mbti?: string;
  isNeutered: boolean;
  photoUrl?: string;
  isMain?: boolean;
  certificationNumber?: string;
  walkingStyles?: string[];
  personalityIds?: number[];
}

export interface PetUpdateRequest {
  name?: string;
  birthDate?: string;
  isNeutered?: boolean;
  mbti?: string;
  photoUrl?: string;
  personalityIds?: number[];
  walkingStyles?: string[];
  walkingStyleCodes?: string[];
}

export interface PetResponse {
  id: number;
  name: string;
  breed: BreedResponse;
  age: number;
  gender: string;
  size: string;
  mbti: string;
  isNeutered: boolean;
  photoUrl: string;
  isMain: boolean;
  isCertified: boolean;
  walkingStyles: string[];
  personalities: PersonalityResponse[];
  createdAt: string;
}

export interface MainPetChangeResponse {
  id: number;
  name: string;
  isMain: boolean;
}

export interface BreedResponse {
  id: number;
  name: string;
  size: string;
}

export interface PersonalityResponse {
  id: number;
  name: string;
  code: string;
}

export interface WalkingStyleResponse {
  id: number;
  name: string;
  code: string;
}

// --- Functions ---

export async function getMyPets(): Promise<PetResponse[]> {
  return apiClient.get<PetResponse[]>('/pets');
}

export async function createPet(data: PetCreateRequest): Promise<PetResponse> {
  return apiClient.post<PetResponse>('/pets', data);
}

export async function updatePet(petId: number, data: PetUpdateRequest): Promise<PetResponse> {
  return apiClient.patch<PetResponse>(`/pets/${petId}`, data);
}

export async function deletePet(petId: number): Promise<void> {
  return apiClient.delete<void>(`/pets/${petId}`);
}

export async function setMainPet(petId: number): Promise<MainPetChangeResponse> {
  return apiClient.patch<MainPetChangeResponse>(`/pets/${petId}/main`);
}

export async function getBreeds(): Promise<BreedResponse[]> {
  return apiClient.get<BreedResponse[]>('/breeds');
}

export async function getPersonalities(): Promise<PersonalityResponse[]> {
  return apiClient.get<PersonalityResponse[]>('/personalities');
}

export async function getWalkingStyles(): Promise<WalkingStyleResponse[]> {
  return apiClient.get<WalkingStyleResponse[]>('/walking-styles');
}
