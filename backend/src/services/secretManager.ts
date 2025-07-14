import { SecretManagerServiceClient } from '@google-cloud/secret-manager';
import { config } from '../config/env';

class SecretManagerService {
  private client: SecretManagerServiceClient;
  private geminiApiKey: string | null = null;

  constructor() {
    this.client = new SecretManagerServiceClient({
      projectId: config.googleCloud.projectId,
      keyFilename: config.googleCloud.credentials,
    });
  }

  async getGeminiApiKey(): Promise<string> {
    if (this.geminiApiKey) {
      return this.geminiApiKey;
    }

    try {
      const name = `projects/${config.googleCloud.projectId}/secrets/${config.secrets.geminiApiKeyName}/versions/latest`;
      const [version] = await this.client.accessSecretVersion({ name });
      
      if (!version.payload?.data) {
        throw new Error('Secret payload is empty');
      }

      const payload = version.payload.data;
      if (typeof payload === 'string') {
        this.geminiApiKey = payload;
      } else {
        this.geminiApiKey = Buffer.from(payload).toString('utf-8');
      }

      return this.geminiApiKey;
    } catch (error) {
      console.error('Error accessing secret:', error);
      
      // Fallback for development - use environment variable
      if (!config.isProduction && process.env.GEMINI_API_KEY) {
        console.warn('Using GEMINI_API_KEY from environment variables (development only)');
        this.geminiApiKey = process.env.GEMINI_API_KEY;
        return this.geminiApiKey;
      }
      
      throw new Error('Failed to retrieve Gemini API key from Secret Manager');
    }
  }

  async refreshApiKey(): Promise<void> {
    this.geminiApiKey = null;
    await this.getGeminiApiKey();
  }
}

export const secretManager = new SecretManagerService();