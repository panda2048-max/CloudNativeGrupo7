import { UserManager, WebStorageStateStore } from "oidc-client-ts";
import { OIDC_CONFIG } from "../config";

export const userManager = new UserManager({
  ...OIDC_CONFIG,
  response_type: "code",
  scope: "openid profile email",
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  automaticSilentRenew: true,
});

export async function getAccessToken() {
  const user = await userManager.getUser();
  if (!user || user.expired) return null;
  return user.access_token;
}
