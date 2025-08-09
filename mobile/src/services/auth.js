import * as SecureStore from 'expo-secure-store';
import AsyncStorage from '@react-native-async-storage/async-storage';

const TOKEN_KEY = 'auth_token_v1';

export async function setToken(token) {
  try {
    await SecureStore.setItemAsync(TOKEN_KEY, token, {
      keychainAccessible: SecureStore.WHEN_UNLOCKED
    });
  } catch (e) {
    await AsyncStorage.setItem(TOKEN_KEY, token);
  }
}

export async function getToken() {
  try {
    const token = await SecureStore.getItemAsync(TOKEN_KEY);
    if (token) return token;
  } catch {}
  return AsyncStorage.getItem(TOKEN_KEY);
}

export async function clearToken() {
  try {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
  } catch {}
  await AsyncStorage.removeItem(TOKEN_KEY);
}